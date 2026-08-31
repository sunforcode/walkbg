package org.example.route.service

import org.example.common.util.IdGenerator
import org.example.route.dto.PoiFilterAgentItem
import org.example.route.dto.PoiFilterAgentRequest
import org.example.route.dto.PoiFilterPreviewItem
import org.example.route.dto.PoiFilterPreviewRequest
import org.example.route.dto.PoiFilterPreviewResponse
import org.example.route.dto.PoiLibrarySaveItem
import org.example.route.dto.PoiLibrarySaveRequest
import org.example.route.dto.PoiLibrarySaveResponse
import org.example.route.dto.PoiResolveAgentLibraryItem
import org.example.route.dto.PoiResolveAgentPoi
import org.example.route.dto.PoiResolveAgentRequest
import org.example.route.model.PoiLibraryItem
import org.example.route.model.Route
import org.example.route.repository.PoiLibraryRepository
import org.example.route.repository.PoiPointRepository
import org.example.route.repository.RouteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 全局 POI 库服务
 *
 * - filterPreview: 调 Agent 用 LLM 筛选路线 POI（仅预览，不落库）
 * - save: 人工确认后入库（按名称+距离去重），同时把对应路线 POI 置为已采纳
 */
@Service
class PoiLibraryService(
    private val poiPointRepository: PoiPointRepository,
    private val poiLibraryRepository: PoiLibraryRepository,
    private val routeRepository: RouteRepository,
    private val kmlAnalysisClientService: KmlAnalysisClientService
) {
    private val logger = LoggerFactory.getLogger(PoiLibraryService::class.java)

    /**
     * AI 筛选预览：把路线 POI 送 Agent 做 LLM 筛选，返回保留/剔除建议与理由。
     * 不修改任何数据，人工确认后走 save。
     */
    @Transactional(readOnly = true)
    fun filterPreview(routeId: String): PoiFilterPreviewResponse {
        val pois = poiPointRepository.findByRouteId(routeId)
        if (pois.isEmpty()) {
            return PoiFilterPreviewResponse(
                routeId = routeId, total = 0, keepCount = 0, rejectCount = 0,
                degraded = false, items = emptyList()
            )
        }

        val agentRequest = PoiFilterAgentRequest(
            routeId = routeId,
            pois = pois.map { poi ->
                PoiFilterAgentItem(
                    name = poi.name,
                    category = poi.category,
                    latitude = poi.latitude,
                    longitude = poi.longitude,
                    elevation = poi.elevation,
                    description = poi.description
                )
            }
        )

        logger.info("POI 筛选预览: routeId=$routeId, 数量=${pois.size}")
        val agentResponse = kmlAnalysisClientService.filterPois(agentRequest)

        // 按 index 回联路线 POI
        val items = agentResponse.results.mapNotNull { result ->
            val poi = pois.getOrNull(result.index) ?: return@mapNotNull null
            PoiFilterPreviewItem(
                poiId = poi.id,
                name = poi.name,
                latitude = poi.latitude,
                longitude = poi.longitude,
                elevation = poi.elevation,
                action = if (result.action == "reject") "reject" else "keep",
                category = result.category ?: poi.category,
                originalCategory = result.originalCategory ?: poi.category,
                reason = result.reason,
                subCategory = poi.subCategory,
                description = poi.description
            )
        }

        return PoiFilterPreviewResponse(
            routeId = routeId,
            total = items.size,
            keepCount = items.count { it.action == "keep" },
            rejectCount = items.count { it.action == "reject" },
            degraded = agentResponse.degraded,
            items = items
        )
    }

    /**
     * 人工确认后入库：
     * - 是否与库内条目为同一位置由 AI 判定（不做硬编码距离合并）：命中则更新库条目，否则新增
     * - AI 判定失败时保守处理为新增（宁可重复，不错误合并）
     * - 同时把对应的路线 POI 置为已采纳（confirmed）
     */
    @Transactional
    fun save(request: PoiLibrarySaveRequest): PoiLibrarySaveResponse {
        val library = poiLibraryRepository.findByStatus("active").toMutableList()
        // 地区来自路线：同地区才允许合并/复用，跨地区不互配
        val route = routeRepository.findById(request.routeId).orElse(null)
        val regionId = route?.regionId
        val regionName = route?.region
        val regionKey = PoiLibraryMatching.regionKey(regionId, regionName)
        val sameRegionLibrary = if (regionKey == null) {
            emptyList()
        } else {
            library.filter { PoiLibraryMatching.regionKey(it.regionId, it.regionName) == regionKey }
        }

        // AI 判定每个入库项是否与库内条目/本批其他项为同一位置：index -> library_id
        // 候选池 = 同地区库条目 + 本批 items（id=batch_i）；每个 POI 召回时排除自身（batch_i），
        // 是否同一位置完全由 LLM 判定，代码不做任何距离阈值合并
        val matchedIds = mutableMapOf<Int, String>()
        val rootByIndex = IntArray(request.items.size) { it }
        if (request.items.isNotEmpty()) {
            try {
                val resolveLibrary = sameRegionLibrary.map {
                    PoiResolveAgentLibraryItem(
                        id = it.id,
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        category = it.category,
                        elevation = it.elevation
                    )
                } + request.items.mapIndexed { i, it ->
                    PoiResolveAgentLibraryItem(
                        id = "batch_$i",
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        category = it.category,
                        elevation = it.elevation
                    )
                }
                val resolveResponse = kmlAnalysisClientService.resolvePoiMatches(
                    PoiResolveAgentRequest(
                        routeId = request.routeId,
                        pois = request.items.mapIndexed { i, it ->
                            PoiResolveAgentPoi(
                                name = it.name,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                elevation = it.elevation,
                                excludeId = "batch_$i"
                            )
                        },
                        library = resolveLibrary
                    )
                )
                val rawMatch = mutableMapOf<Int, String?>()
                resolveResponse.results.forEach { r -> rawMatch[r.index] = r.libraryId }
                // 并查集分组：AI 判定 batch_i -> batch_j 即视为同一位置；组内任一成员指向库条目，
                // 则全组归并该库条目；无库指向的组以最小 index 为代表新增，其余成员归并代表
                val parent = IntArray(request.items.size) { it }
                fun find(x: Int): Int {
                    var root = x
                    while (parent[root] != root) root = parent[root]
                    var cur = x
                    while (parent[cur] != cur) {
                        val next = parent[cur]
                        parent[cur] = root
                        cur = next
                    }
                    return root
                }
                fun union(a: Int, b: Int) {
                    val ra = find(a)
                    val rb = find(b)
                    if (ra != rb) parent[maxOf(ra, rb)] = minOf(ra, rb)
                }
                request.items.indices.forEach { i ->
                    val t = rawMatch[i]
                    if (t != null && t.startsWith("batch_")) {
                        t.removePrefix("batch_").toIntOrNull()?.let { j ->
                            if (j in request.items.indices && j != i) union(i, j)
                        }
                    }
                }
                val groupLib = mutableMapOf<Int, String>()
                request.items.indices.forEach { i ->
                    val t = rawMatch[i]
                    if (t != null && !t.startsWith("batch_") && library.any { it.id == t }) {
                        groupLib.putIfAbsent(find(i), t)
                    }
                }
                request.items.indices.forEach { i ->
                    rootByIndex[i] = find(i)
                    groupLib[find(i)]?.let { libId -> matchedIds[i] = libId }
                }
                logger.info(
                    "POI 入库位置 AI 判定完成: routeId=${request.routeId}, " +
                        "命中=${matchedIds.size}/${request.items.size}, degraded=${resolveResponse.degraded}"
                )
            } catch (e: Exception) {
                logger.warn("POI 入库位置 AI 判定失败，本批全部按新增处理: ${e.message}")
            }
        }

        var saved = 0
        var updated = 0
        var confirmedPois = 0
        val newItemsByRoot = mutableMapOf<Int, PoiLibraryItem>()

        request.items.forEachIndexed { index, item ->
            // AI 判定归并：命中库条目则更新库条目；同组无库指向时归并到组代表新建的条目；
            // 未命中/判定失败一律新增
            val match = matchedIds[index]?.let { libId -> library.firstOrNull { it.id == libId } }
                ?: newItemsByRoot[rootByIndex[index]]
            if (match != null) {
                match.category = item.category
                match.elevation = item.elevation ?: match.elevation
                match.subCategory = item.subCategory ?: match.subCategory
                match.description = item.description ?: match.description
                match.aiReason = item.aiReason ?: match.aiReason
                match.updatedAt = Instant.now()
                poiLibraryRepository.save(match)
                updated++
                logger.debug("POI 库合并更新条目: ${match.id} (${match.name}) <- ${item.name}")
            } else {
                val newItem = PoiLibraryItem(
                    id = "pl_${IdGenerator.generateShortId()}",
                    name = item.name.trim(),
                    latitude = item.latitude,
                    longitude = item.longitude,
                    elevation = item.elevation,
                    category = item.category,
                    regionId = regionId,
                    regionName = regionName,
                    subCategory = item.subCategory,
                    description = item.description,
                    aiReason = item.aiReason,
                    sourceRouteId = request.routeId,
                    status = "active"
                )
                poiLibraryRepository.save(newItem)
                library.add(newItem)
                newItemsByRoot.putIfAbsent(rootByIndex[index], newItem)
                saved++
                logger.debug("POI 库新增条目: ${newItem.id} (${newItem.name})")
            }

            // 回写路线 POI 状态为已采纳
            if (!item.poiId.isNullOrBlank()) {
                poiPointRepository.findById(item.poiId).orElse(null)?.let { poi ->
                    if (poi.status != "confirmed") {
                        poi.status = "confirmed"
                        poiPointRepository.save(poi)
                        confirmedPois++
                    }
                }
            }
        }

        logger.info(
            "POI 入库完成: routeId=${request.routeId}, 新增=$saved, 合并更新=$updated, 采纳回写=$confirmedPois"
        )
        return PoiLibrarySaveResponse(saved = saved, updated = updated, confirmedPois = confirmedPois)
    }

    /** 查询全部库内条目 */
    @Transactional(readOnly = true)
    fun list(): List<PoiLibraryItem> = poiLibraryRepository.findByStatus("active")

    /** 从库中移除条目（软删除，status 置为 inactive） */
    @Transactional
    fun remove(id: String): Boolean {
        val item = poiLibraryRepository.findById(id).orElse(null) ?: return false
        item.status = "inactive"
        item.updatedAt = Instant.now()
        poiLibraryRepository.save(item)
        logger.info("POI 库条目已移除: $id (${item.name})")
        return true
    }
}
