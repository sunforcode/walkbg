package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.util.IdGenerator
import org.example.route.dto.CallbackPoiPointDto
import org.example.route.dto.CallbackSegmentDto
import org.example.route.dto.CallbackSegmentSchemeDto
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.example.route.model.PoiPoint
import org.example.route.model.RouteMapData
import org.example.route.model.Segment
import org.example.route.model.SegmentScheme
import org.example.route.model.Waypoint
import org.example.route.repository.PoiPointRepository
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
        
        logger.info(
            "处理 KML 分析回调，routeId: $routeId, " +
            "segmentSchemes: ${request.segmentSchemes.size}, " +
            "poiPoints: ${request.poiPoints.size}"
        )
        
        // 更新 Route 实体主体信息
        request.generatedDescription?.let { route.description = it }
        request.estimatedDifficulty?.let { route.difficulty = it }
        request.isLoop?.let { route.isLoop = it }

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

        // 清除旧数据
        deleteExistingData(routeId)

        // 保存分段方案
        if (request.segmentSchemes.isNotEmpty()) {
            saveSegmentSchemes(routeId, request.segmentSchemes)
            logger.info("已保存 segmentSchemes: ${request.segmentSchemes.size} 个方案")
        }

        // 保存 POI 点
        if (request.poiPoints.isNotEmpty()) {
            savePoiPoints(routeId, request.poiPoints)
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
                    currentStep = "分析完成"
                )
            )
        } else {
            logger.warn("回调中 taskId 为空，无法向 SSE 总线发布 completed 事件")
        }
    }

    private fun deleteExistingData(routeId: String) {
        val deletedSchemes = segmentSchemeRepository.deleteByRouteId(routeId)
        if (deletedSchemes > 0) logger.info("删除路线 $routeId 的 $deletedSchemes 个旧分段方案")

        val existingSegments = segmentRepository.findByRouteId(routeId)
        if (existingSegments.isNotEmpty()) {
            logger.info("删除路线 $routeId 的 ${existingSegments.size} 个旧分段")
            segmentRepository.deleteAll(existingSegments)
        }

        val deletedPois = poiPointRepository.deleteByRouteId(routeId)
        if (deletedPois > 0) logger.info("删除路线 $routeId 的 $deletedPois 个旧 POI 点")
    }

    // =========================================================================
    // segment_schemes 落库
    // =========================================================================

    private fun saveSegmentSchemes(routeId: String, schemes: List<CallbackSegmentSchemeDto>) {
        schemes.forEach { schemeDto ->
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
        }
    }

    // =========================================================================
    // poi_points 落库
    // =========================================================================

    private fun savePoiPoints(routeId: String, poiPoints: List<CallbackPoiPointDto>) {
        poiPoints.forEach { dto ->
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
                cardData = cardDataJson
            )
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
        val segmentId = if (segDto.id.startsWith("seg_")) {
            "seg_${idGenerator.generateShortId()}"
        } else {
            segDto.id
        }
        
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
