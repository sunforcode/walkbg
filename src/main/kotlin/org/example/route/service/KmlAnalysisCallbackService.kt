package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.util.IdGenerator
import org.example.route.dto.CallbackPoiPointDto
import org.example.route.dto.CallbackSegmentDto
import org.example.route.dto.CallbackSegmentSchemeDto
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.dto.PoiResolveAgentLibraryItem
import org.example.route.dto.PoiResolveAgentPoi
import org.example.route.dto.PoiResolveAgentRequest
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.example.route.model.PoiPoint
import org.example.route.model.Route
import org.example.route.model.RouteMapData
import org.example.route.model.Segment
import org.example.route.model.SegmentScheme
import org.example.route.model.Waypoint
import org.example.route.repository.PoiPointRepository
import org.example.route.repository.PoiLibraryRepository
import org.example.route.repository.RouteMapDataRepository
import org.example.route.repository.RouteRepository
import org.example.route.repository.SegmentRepository
import org.example.route.repository.SegmentSchemeRepository
import org.example.route.repository.WaypointRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class KmlAnalysisCallbackService(
private val routeRepository: RouteRepository,
private val segmentRepository: SegmentRepository,
private val segmentSchemeRepository: SegmentSchemeRepository,
private val poiPointRepository: PoiPointRepository,
private val poiLibraryRepository: PoiLibraryRepository,
private val kmlAnalysisClientService: KmlAnalysisClientService,
private val routeMapDataRepository: RouteMapDataRepository,
private val waypointRepository: WaypointRepository,
private val sseTaskEventBus: SseTaskEventBus,
private val objectMapper: ObjectMapper = ObjectMapper()
) {
    private val idGenerator = IdGenerator
    private val logger = LoggerFactory.getLogger(KmlAnalysisCallbackService::class.java)

    @Transactional
    fun handleCallback(request: KmlAnalysisCallbackRequest) {
        val routeId = request.routeId

        if (routeId.isNullOrBlank()) {
            logger.warn("收到 KML 分析回调，但 routeId 为空，taskId: ${request.taskId}")
            throw IllegalArgumentException("routeId 不能为空")
        }

        val route = routeRepository.findById(routeId).orElseThrow {
            IllegalArgumentException("路线不存在: $routeId")
        }

        if (request.status == "failed") {
            handleFailedCallback(route, request)
            return
        }

        logger.info(
            "处理 KML 分析回调，routeId: $routeId, " +
            "segmentSchemes: ${request.segmentSchemes.size}, " +
            "poiPoints: ${request.poiPoints.size}"
        )
        
        // 更新 Route 实体主体信息
        request.generatedDescription?.let { route.description = it }
        request.estimatedDifficulty?.let { route.difficulty = it }
        request.isLoop?.let { route.isLoop = it }

        // 保存完整轨迹路径（紧凑 JSON），索引与分段 track_start/end_index 对齐，供前端精确渲染
        if (request.trackPath.isNotEmpty()) {
            route.trackGeoJson = objectMapper.writeValueAsString(request.trackPath)
            logger.info("已保存路线 $routeId 的完整轨迹路径（${request.trackPath.size} 个点）")
        }

        // 分析完成后，将状态从"分析中"恢复为"规划中"，等待管理员发布
        if (route.status == 3) {
            route.markAnalysisComplete()
            logger.info("路线 $routeId 分析完成，状态更新为规划中")
        }

        route.updatedAt = Instant.now()
        routeRepository.save(route)

        // 更新/创建 RouteMapData 记录
        val mapData = routeMapDataRepository.findById(routeId).orElse(null)
        if (mapData != null) {
            val updatedMapData = mapData.copy(
                distance = request.totalDistanceKm?.let { BigDecimal.valueOf(it) },
                elevationGain = request.totalElevationGainM?.let { BigDecimal.valueOf(it) },
                elevationLoss = request.totalElevationLossM?.let { BigDecimal.valueOf(it) },
                kmlUrl = request.sourceKmlUrl ?: mapData.kmlUrl,
                updatedAt = Instant.now()
            )
            routeMapDataRepository.save(updatedMapData)
        } else {
            val newMapData = RouteMapData(
                id = routeId,
                distance = request.totalDistanceKm?.let { BigDecimal.valueOf(it) },
                elevationGain = request.totalElevationGainM?.let { BigDecimal.valueOf(it) },
                elevationLoss = request.totalElevationLossM?.let { BigDecimal.valueOf(it) },
                kmlUrl = request.sourceKmlUrl,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            routeMapDataRepository.save(newMapData)
        }

        // 清除旧草稿数据（保留人工确认的 confirmed 数据，避免重新分析覆盖人工成果）
        deleteExistingDraftData(routeId)

        // 保存分段方案
        if (request.segmentSchemes.isNotEmpty()) {
            saveSegmentSchemes(routeId, request.segmentSchemes)
            logger.info("已保存 segmentSchemes: ${request.segmentSchemes.size} 个方案")
        }

        // 保存 POI 点
        if (request.poiPoints.isNotEmpty()) {
            savePoiPoints(route, request.poiPoints)
            logger.info("已保存 poiPoints: ${request.poiPoints.size}")
        }

        logger.info(
            "完成 KML 分析回调处理，routeId: $routeId, " +
            "segmentSchemes: ${request.segmentSchemes.size}, " +
            "poiPoints: ${request.poiPoints.size}"
        )

        // 落库成功后向 SSE 总线发布 completed 事件
        val taskId = request.taskId
        if (!taskId.isNullOrBlank()) {
            logger.info("向 SSE 总线发布 completed 事件，taskId=$taskId, routeId=$routeId")
            sseTaskEventBus.publish(
                taskId,
                SseProgressEvent(
                    taskId = taskId,
                    status = "completed",
                    progress = 100,
                    routeId = routeId,
                    currentStep = "分析完成",
                    degraded = request.degraded
                )
            )
        } else {
            logger.warn("回调中 taskId 为空，无法向 SSE 总线发布 completed 事件")
        }
    }

    /**
     * 处理分析失败的回调。
     *
     * 与成功回调的区别：
     * 1. 不写入 description/difficulty/isLoop 等分析产出字段（agent 分析失败时这些数据本身就是空/无意义的）；
     * 2. 不清除/保存 segmentSchemes、poiPoints，保留路线上一次的有效数据（如果有）；
     * 3. 仅把路线状态从"分析中"释放为"规划中"，允许重新发起分析；
     * 4. 向 SSE 总线发布真正的 failed 事件，而不是之前硬编码的 completed 事件。
     */
    private fun handleFailedCallback(route: Route, request: KmlAnalysisCallbackRequest) {
        val routeId = request.routeId
        logger.warn("KML 分析失败回调，routeId: $routeId, taskId: ${request.taskId}")

        if (route.status == 3) {
            route.markAnalysisFailed()
            logger.info("路线 $routeId 分析失败，状态恢复为规划中，允许重新发起分析")
        }
        route.updatedAt = Instant.now()
        routeRepository.save(route)

        val taskId = request.taskId
        if (!taskId.isNullOrBlank()) {
            logger.info("向 SSE 总线发布 failed 事件，taskId=$taskId, routeId=$routeId")
            sseTaskEventBus.publish(
                taskId,
                SseProgressEvent(
                    taskId = taskId,
                    status = "failed",
                    progress = 100,
                    routeId = routeId,
                    currentStep = "分析失败",
                    error = request.error ?: "分析失败"
                )
            )
        } else {
            logger.warn("回调中 taskId 为空，无法向 SSE 总线发布 failed 事件")
        }
    }

    /**
     * 只删除草稿（draft）数据，保留人工确认（confirmed）的路段和 POI。
     * 分段方案：若方案下已无任何路段（草稿删光且无已确认路段），则一并删除。
     */
    private fun deleteExistingDraftData(routeId: String) {
        val draftSegments = segmentRepository.findByRouteIdAndStatus(routeId, "draft")
        if (draftSegments.isNotEmpty()) {
            logger.info("删除路线 $routeId 的 ${draftSegments.size} 个旧草稿分段")
            segmentRepository.deleteAll(draftSegments)
        }

        // 清理空方案：删除后不再被任何路段引用的方案
        val remainingSegments = segmentRepository.findByRouteId(routeId)
        val usedSchemeIds = remainingSegments.mapNotNull { it.schemeId }.toSet()
        val schemes = segmentSchemeRepository.findByRouteId(routeId)
        val orphanSchemes = schemes.filter { it.id !in usedSchemeIds }
        if (orphanSchemes.isNotEmpty()) {
            logger.info("删除路线 $routeId 的 ${orphanSchemes.size} 个空分段方案")
            segmentSchemeRepository.deleteAll(orphanSchemes)
        }

        val draftPois = poiPointRepository.findByRouteIdAndStatus(routeId, "draft")
        if (draftPois.isNotEmpty()) {
            logger.info("删除路线 $routeId 的 ${draftPois.size} 个旧草稿 POI 点")
            poiPointRepository.deleteAll(draftPois)
        }
    }

    // =========================================================================
    // segment_schemes 落库
    // =========================================================================

    private fun saveSegmentSchemes(routeId: String, schemes: List<CallbackSegmentSchemeDto>) {
        schemes.forEach { schemeDto ->
            // 同类型方案替换：删除旧的同类型方案（迁移已确认路段），避免方案重复堆积
            replaceOldSchemesOfType(routeId, schemeDto.schemeType, schemeDto.label)

            val schemeId = "scheme_${idGenerator.generateShortId()}"
            val scheme = SegmentScheme(
                id = schemeId,
                routeId = routeId,
                schemeType = schemeDto.schemeType,
                label = schemeDto.label,
                isDefault = schemeDto.isDefault
            )
            segmentSchemeRepository.save(scheme)
            logger.debug("保存分段方案: $schemeId, type=${schemeDto.schemeType}, segments=${schemeDto.segments.size}")

            schemeDto.segments.forEachIndexed { index, segDto ->
                val segment = createSegmentFromDto(routeId, segDto, index, schemeId, schemeDto.schemeType)
                segmentRepository.save(segment)
            }

            // 回填：把旧方案迁移过来的已确认路段挂载到新方案
            val migrating = segmentRepository.findByRouteIdAndStatus(routeId, "confirmed")
                .filter { it.schemeId == "__migrating__" }
            if (migrating.isNotEmpty()) {
                migrating.forEach { it.schemeId = schemeId }
                segmentRepository.saveAll(migrating)
                logger.info("已将 ${migrating.size} 个迁移路段挂载到新方案 $schemeId")
            }
        }
    }

    /**
     * 同类型方案替换：
     * 1. 删除旧同类型方案下的草稿路段
     * 2. 已确认（confirmed）路段迁移挂载到新方案（由调用方在创建新方案后回填 schemeId）
     * 3. 删除旧方案本身
     */
    private fun replaceOldSchemesOfType(routeId: String, schemeType: String, newLabel: String) {
        val oldSchemes = segmentSchemeRepository.findByRouteId(routeId)
            .filter { it.schemeType == schemeType }
        if (oldSchemes.isEmpty()) return

        oldSchemes.forEach { oldScheme ->
            val oldSegments = segmentRepository.findByRouteId(routeId)
                .filter { it.schemeId == oldScheme.id }
            val confirmedSegments = oldSegments.filter { it.status == "confirmed" }
            val draftSegments = oldSegments.filter { it.status == "draft" }

            // 迁移已确认路段到新方案
            if (confirmedSegments.isNotEmpty()) {
                confirmedSegments.forEach { it.schemeId = "__migrating__" }
                segmentRepository.saveAll(confirmedSegments)
                logger.info("方案[${oldScheme.label}]的 ${confirmedSegments.size} 个已确认路段将迁移到新方案[$newLabel]")
            }
            // 删除旧草稿路段
            if (draftSegments.isNotEmpty()) {
                segmentRepository.deleteAll(draftSegments)
            }
            // 删除旧方案
            segmentSchemeRepository.delete(oldScheme)
            logger.info("删除旧同类型方案[${oldScheme.label}] (type=$schemeType)")
        }
    }

    // =========================================================================
    // poi_points 落库
    // =========================================================================

private fun savePoiPoints(route: Route, poiPoints: List<CallbackPoiPointDto>) {
val routeId = route.id!!
// 全局 POI 库：同地区条目作为候选，是否同一位置由 AI 判定（不做硬编码距离合并）
val library = poiLibraryRepository.findByStatus("active")
val regionKey = PoiLibraryMatching.regionKey(route.regionId, route.region)
val sameRegionLibrary = if (regionKey == null) {
emptyList()
} else {
library.filter { PoiLibraryMatching.regionKey(it.regionId, it.regionName) == regionKey }
}

// AI 判定每个 POI 是否与库内条目同一位置：index -> library_id
val matchedIds = mutableMapOf<Int, String>()
if (poiPoints.isNotEmpty() && sameRegionLibrary.isNotEmpty()) {
try {
val resolveResponse = kmlAnalysisClientService.resolvePoiMatches(
PoiResolveAgentRequest(
routeId = routeId,
pois = poiPoints.map {
PoiResolveAgentPoi(
name = it.name,
latitude = it.latitude,
longitude = it.longitude,
elevation = it.elevation
)
},
library = sameRegionLibrary.map {
PoiResolveAgentLibraryItem(
id = it.id,
name = it.name,
latitude = it.latitude,
longitude = it.longitude,
category = it.category,
elevation = it.elevation
)
}
)
)
resolveResponse.results.forEach { r ->
r.libraryId?.let { libId -> matchedIds[r.index] = libId }
}
logger.info(
"POI 位置 AI 判定完成: routeId=$routeId, " +
"命中=${matchedIds.size}/${poiPoints.size}, degraded=${resolveResponse.degraded}"
)
} catch (e: Exception) {
logger.warn("POI 位置 AI 判定失败，本批全部保持草稿: ${e.message}")
}
}

poiPoints.forEachIndexed { index, dto ->
val cardDataJson = dto.cardData?.let { objectMapper.writeValueAsString(it) }
val poi = PoiPoint(
id = "poi_${idGenerator.generateShortId()}",
routeId = routeId,
name = dto.name,
latitude = dto.latitude,
longitude = dto.longitude,
elevation = dto.elevation,
category = dto.category,
subCategory = dto.subCategory,
source = dto.source,
description = dto.description,
confidence = dto.confidence,
cardData = cardDataJson,
status = "draft"  // 分析结果写入为草稿，AI 判定命中库内条目才自动采纳
)
val matched = matchedIds[index]?.let { libId ->
sameRegionLibrary.firstOrNull { it.id == libId }
}
if (matched != null) {
poi.status = "confirmed"
logger.info(
"AI 判定命中全局库条目 ${matched.id}(${matched.name})，自动采纳: ${poi.name}"
)
}
poiPointRepository.save(poi)
logger.debug("保存 POI 点: ${poi.id}, category=${poi.category}, source=${poi.source}")
}
}

    private fun createSegmentFromDto(
        routeId: String,
        segDto: CallbackSegmentDto,
        index: Int,
        schemeId: String? = null,
        schemeType: String? = null
    ): Segment {
        // 始终生成新 ID：Agent 回调的路段 ID 每次分析都相同，
        // 若直接复用会覆盖（JPA merge）已有路段——包括已采纳的 confirmed 数据
        val segmentId = "seg_${idGenerator.generateShortId()}"
        
        val startPointId = segDto.startPoint?.let { sp ->
            val wp = Waypoint(
                id = "wp_${idGenerator.generateShortId()}",
                routeId = routeId,
                name = "${segDto.name} 起点",
                latitude = sp.latitude,
                longitude = sp.longitude,
                elevation = sp.elevation,
                type = "segment_start",
                sequenceNumber = index * 2
            )
            waypointRepository.save(wp)
            wp.id
        }

        val endPointId = segDto.endPoint?.let { ep ->
            val wp = Waypoint(
                id = "wp_${idGenerator.generateShortId()}",
                routeId = routeId,
                name = "${segDto.name} 终点",
                latitude = ep.latitude,
                longitude = ep.longitude,
                elevation = ep.elevation,
                type = "segment_end",
                sequenceNumber = index * 2 + 1
            )
            waypointRepository.save(wp)
            wp.id
        }

        return Segment(
            id = segmentId,
            routeId = routeId,
            name = segDto.name,
            description = segDto.description,
            distance = segDto.distance,
            elevationGain = segDto.elevationGain,
            elevationLoss = segDto.elevationLoss,
            estimatedTime = segDto.estimatedTime.toDouble(),
            difficulty = segDto.difficulty,
            routeType = null,
            notes = buildNotesFromDto(segDto),
            startPointId = startPointId,
            endPointId = endPointId,
            sequenceNumber = segDto.sequenceNumber,
            trackStartIndex = segDto.trackStartIndex,
            trackEndIndex = segDto.trackEndIndex,
            color = segDto.color,
            schemeId = schemeId,
            schemeType = schemeType,
            status = "draft",  // 分析结果写入为草稿，等待人工采纳
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun buildNotesFromDto(segDto: CallbackSegmentDto): String? {
        val notesParts = mutableListOf<String>()
        
        segDto.segmentType?.let { notesParts.add("路段类型: $it") }
        segDto.slopeDirection?.let { notesParts.add("坡度方向: $it") }
        segDto.avgSlopeDegrees?.let { notesParts.add("平均坡度: ${String.format("%.1f", it)}°") }
        segDto.maxSlopeDegrees?.let { notesParts.add("最大坡度: ${String.format("%.1f", it)}°") }
        segDto.confidence?.let { notesParts.add("分析置信度: ${String.format("%.0f", it * 100)}%") }
        
        if (segDto.notes != null) {
            notesParts.add(segDto.notes!!)
        }
        
        return if (notesParts.isNotEmpty()) {
            notesParts.joinToString("; ")
        } else {
            null
        }
    }

    fun triggerKmlAnalysis(routeId: String, kmlUrl: String): String {
        logger.info("触发 KML 分析，routeId: $routeId, kmlUrl: $kmlUrl")
        return "task_${idGenerator.generateShortId()}"
    }
}
