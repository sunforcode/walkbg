package org.example.route.service

import org.example.common.util.IdGenerator
import org.example.route.dto.PoiPointDto
import org.example.route.dto.SegmentDto
import org.example.route.model.Segment
import org.example.route.model.Waypoint
import org.example.route.repository.PoiPointRepository
import org.example.route.repository.SegmentRepository
import org.example.route.repository.WaypointRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 路段/POI 人工编辑服务
 *
 * 支持后台管理工作流：分析结果作为草稿（draft）写入，
 * 管理员可拆分路段、采纳（confirm）草稿数据。
 */
@Service
class SegmentEditService(
    private val segmentRepository: SegmentRepository,
    private val poiPointRepository: PoiPointRepository,
    private val waypointRepository: WaypointRepository
) {
    private val logger = LoggerFactory.getLogger(SegmentEditService::class.java)

    /**
     * 拆分路段：在指定轨迹索引处把一段拆为两段
     *
     * @param splitTrackIndex 新路段（后段）的起始轨迹索引，需满足 start < splitTrackIndex <= end
     * @param splitPoint 拆分点坐标（可选，前端可从轨迹点中选择传入）
     */
    @Transactional
    fun splitSegment(
        routeId: String,
        segmentId: String,
        splitTrackIndex: Int,
        splitPoint: SplitPointRequest?
    ): Pair<SegmentDto, SegmentDto> {
        val segment = segmentRepository.findById(segmentId).orElseThrow {
            IllegalArgumentException("路段不存在: $segmentId")
        }
        if (segment.routeId != routeId) {
            throw IllegalArgumentException("路段 $segmentId 不属于路线 $routeId")
        }

        val startIdx = segment.trackStartIndex
        val endIdx = segment.trackEndIndex
        if (startIdx == null || endIdx == null) {
            throw IllegalArgumentException("路段缺少轨迹索引信息，无法拆分")
        }
        if (splitTrackIndex <= startIdx || splitTrackIndex > endIdx) {
            throw IllegalArgumentException("拆分索引 $splitTrackIndex 必须在 (${startIdx}, ${endIdx}] 范围内")
        }

        // 按轨迹索引比例拆分距离/海拔（无完整轨迹数据时的估算）
        val totalSpan = (endIdx - startIdx).coerceAtLeast(1)
        val ratio = (splitTrackIndex - startIdx).toDouble() / totalSpan

        val segADistance = segment.distance?.let { it * ratio }
        val segBDistance = segment.distance?.let { it * (1 - ratio) }
        val segAGain = segment.elevationGain?.let { it * ratio }
        val segBGain = segment.elevationGain?.let { it * (1 - ratio) }
        val segALoss = segment.elevationLoss?.let { it * ratio }
        val segBLoss = segment.elevationLoss?.let { it * (1 - ratio) }
        val segATime = segment.estimatedTime?.let { it * ratio }
        val segBTime = segment.estimatedTime?.let { it * (1 - ratio) }

        // 创建拆分点 waypoint
        val splitWaypointId = splitPoint?.let { sp ->
            val wp = Waypoint(
                id = IdGenerator.generateIdWithPrefix("waypoint"),
                routeId = routeId,
                name = sp.name ?: "${segment.name} 拆分点",
                latitude = sp.latitude,
                longitude = sp.longitude,
                elevation = sp.elevation,
                type = "segment_split",
                sequenceNumber = splitTrackIndex,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            waypointRepository.save(wp)
            wp.id
        }

        // 前段：复用原路段记录
        val segA = segment.copy(
            name = segment.name,
            trackEndIndex = splitTrackIndex - 1,
            distance = segADistance,
            elevationGain = segAGain,
            elevationLoss = segALoss,
            estimatedTime = segATime,
            endPointId = splitWaypointId ?: segment.endPointId,
            notes = appendSplitNote(segment.notes, "由路段拆分而来（前段）"),
            updatedAt = Instant.now()
        )
        val savedA = segmentRepository.save(segA)

        // 后段：新建记录
        val segB = Segment(
            id = IdGenerator.generateIdWithPrefix("segment"),
            routeId = routeId,
            name = segment.name,
            description = segment.description,
            distance = segBDistance,
            elevationGain = segBGain,
            elevationLoss = segBLoss,
            estimatedTime = segBTime,
            difficulty = segment.difficulty,
            routeType = segment.routeType,
            notes = appendSplitNote(segment.notes, "由路段拆分而来（后段）"),
            startPointId = splitWaypointId ?: segment.startPointId,
            endPointId = segment.endPointId,
            sequenceNumber = segment.sequenceNumber + 1,
            trackStartIndex = splitTrackIndex,
            trackEndIndex = endIdx,
            color = segment.color,
            schemeId = segment.schemeId,
            schemeType = segment.schemeType,
            status = segment.status,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val savedB = segmentRepository.save(segB)

        logger.info("路段 $segmentId 已拆分为 ${savedA.id} + ${savedB.id}，拆分索引: $splitTrackIndex")
        return Pair(SegmentDto.fromSegment(savedA), SegmentDto.fromSegment(savedB))
    }

    /** 采纳单个草稿路段 */
    @Transactional
    fun adoptSegment(routeId: String, segmentId: String): SegmentDto {
        val segment = segmentRepository.findById(segmentId).orElseThrow {
            IllegalArgumentException("路段不存在: $segmentId")
        }
        if (segment.routeId != routeId) {
            throw IllegalArgumentException("路段 $segmentId 不属于路线 $routeId")
        }
        segment.status = "confirmed"
        segment.updatedAt = Instant.now()
        val saved = segmentRepository.save(segment)
        logger.info("路段 $segmentId 已采纳为正式数据")
        return SegmentDto.fromSegment(saved)
    }

    /** 采纳路线的全部草稿路段 */
    @Transactional
    fun adoptAllSegments(routeId: String): Int {
        val drafts = segmentRepository.findByRouteIdAndStatus(routeId, "draft")
        drafts.forEach {
            it.status = "confirmed"
            it.updatedAt = Instant.now()
        }
        segmentRepository.saveAll(drafts)
        logger.info("路线 $routeId 批量采纳 ${drafts.size} 个草稿路段")
        return drafts.size
    }

    /** 采纳单个草稿 POI */
    @Transactional
    fun adoptPoi(routeId: String, poiId: String): PoiPointDto {
        val poi = poiPointRepository.findById(poiId).orElseThrow {
            IllegalArgumentException("POI 不存在: $poiId")
        }
        if (poi.routeId != routeId) {
            throw IllegalArgumentException("POI $poiId 不属于路线 $routeId")
        }
        poi.status = "confirmed"
        val saved = poiPointRepository.save(poi)
        logger.info("POI $poiId 已采纳为正式数据")
        return PoiPointDto.fromPoiPoint(saved)
    }

    /** 采纳路线的全部草稿 POI */
    @Transactional
    fun adoptAllPois(routeId: String): Int {
        val drafts = poiPointRepository.findByRouteIdAndStatus(routeId, "draft")
        drafts.forEach { it.status = "confirmed" }
        poiPointRepository.saveAll(drafts)
        logger.info("路线 $routeId 批量采纳 ${drafts.size} 个草稿 POI")
        return drafts.size
    }

    /**
     * 合并多个路段为一个：轨迹区间取并集（首尾延伸），
     * 距离/爬升/下降/用时累加，名称默认取第一个路段（可自定义）
     */
    @Transactional
    fun mergeSegments(routeId: String, segmentIds: List<String>, name: String?): SegmentDto {
        if (segmentIds.size < 2) {
            throw IllegalArgumentException("至少选择两个路段才能合并")
        }
        val segments = segmentIds.map { id ->
            val seg = segmentRepository.findById(id).orElseThrow {
                IllegalArgumentException("路段不存在: $id")
            }
            if (seg.routeId != routeId) {
                throw IllegalArgumentException("路段 $id 不属于路线 $routeId")
            }
            seg
        }.sortedBy { it.trackStartIndex ?: Int.MAX_VALUE }

        val schemeIds = segments.mapNotNull { it.schemeId }.toSet()
        if (schemeIds.size > 1) {
            throw IllegalArgumentException("所选路段必须属于同一分段方案")
        }

        val first = segments.first()
        val last = segments.last()

        val merged = first.copy(
            name = name?.takeIf { it.isNotBlank() } ?: first.name,
            trackEndIndex = last.trackEndIndex ?: first.trackEndIndex,
            distance = segments.mapNotNull { it.distance }.takeIf { it.isNotEmpty() }?.sum(),
            elevationGain = segments.mapNotNull { it.elevationGain }.takeIf { it.isNotEmpty() }?.sum(),
            elevationLoss = segments.mapNotNull { it.elevationLoss }.takeIf { it.isNotEmpty() }?.sum(),
            estimatedTime = segments.mapNotNull { it.estimatedTime }.takeIf { it.isNotEmpty() }?.sum(),
            notes = appendSplitNote(first.notes, "由 ${segments.size} 个路段手动合并而来"),
            updatedAt = Instant.now()
        )
        val saved = segmentRepository.save(merged)
        segmentRepository.deleteAll(segments.drop(1))
        logger.info("路线 $routeId 合并 ${segments.size} 个路段为 ${saved.id}")
        return SegmentDto.fromSegment(saved)
    }

    /** 路段改名 */
    @Transactional
    fun renameSegment(routeId: String, segmentId: String, newName: String): SegmentDto {
        val segment = segmentRepository.findById(segmentId).orElseThrow {
            IllegalArgumentException("路段不存在: $segmentId")
        }
        if (segment.routeId != routeId) {
            throw IllegalArgumentException("路段 $segmentId 不属于路线 $routeId")
        }
        segment.name = newName
        segment.updatedAt = Instant.now()
        val saved = segmentRepository.save(segment)
        logger.info("路段 $segmentId 已改名为「$newName」")
        return SegmentDto.fromSegment(saved)
    }

    /** POI 改名 */
    @Transactional
    fun renamePoi(routeId: String, poiId: String, newName: String): PoiPointDto {
        val poi = poiPointRepository.findById(poiId).orElseThrow {
            IllegalArgumentException("POI 不存在: $poiId")
        }
        if (poi.routeId != routeId) {
            throw IllegalArgumentException("POI $poiId 不属于路线 $routeId")
        }
        poi.name = newName
        val saved = poiPointRepository.save(poi)
        logger.info("POI $poiId 已改名为「$newName」")
        return PoiPointDto.fromPoiPoint(saved)
    }

    private fun appendSplitNote(notes: String?, note: String): String? =
        if (notes.isNullOrBlank()) note else "$notes; $note"
}

/**
 * 拆分点请求（坐标可选传入，便于在轨迹上精确定位）
 */
data class SplitPointRequest(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val name: String? = null
)
