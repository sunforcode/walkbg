package org.example.route.service

import org.example.common.util.IdGenerator
import org.example.route.dto.CallbackCampsiteDto
import org.example.route.dto.CallbackMarkerPointDto
import org.example.route.dto.CallbackSegmentDto
import org.example.route.dto.CallbackSupplyDto
import org.example.route.dto.CallbackWaterSourceDto
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.model.Campsite
import org.example.route.model.CampsiteType
import org.example.route.model.MarkerPoint
import org.example.route.model.MarkerPointType
import org.example.route.model.Segment
import org.example.route.model.Supply
import org.example.route.repository.CampsiteRepository
import org.example.route.repository.MarkerPointRepository
import org.example.route.repository.RouteRepository
import org.example.route.repository.SegmentRepository
import org.example.route.repository.SupplyRepository
import org.example.water.model.WaterSource
import org.example.water.model.WaterSourceType
import org.example.water.repository.WaterSourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class KmlAnalysisCallbackService(
    private val routeRepository: RouteRepository,
    private val segmentRepository: SegmentRepository,
    private val waterSourceRepository: WaterSourceRepository,
    private val campsiteRepository: CampsiteRepository,
    private val supplyRepository: SupplyRepository,
    private val markerPointRepository: MarkerPointRepository,
    private val idGenerator: IdGenerator
) {
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
            "segments: ${request.segments.size}, " +
            "waterSources: ${request.waterSources.size}, " +
            "campsites: ${request.campsites.size}, " +
            "supplies: ${request.supplies.size}, " +
            "markerPoints: ${request.markerPoints.size}"
        )
        
        deleteExistingPOIs(routeId)
        
        saveSegments(routeId, request.segments)
        saveWaterSources(routeId, request.waterSources)
        saveCampsites(routeId, request.campsites)
        saveSupplies(routeId, request.supplies)
        saveMarkerPoints(routeId, request.markerPoints)
        
        logger.info(
            "完成 KML 分析回调处理，routeId: $routeId, " +
            "已保存 segments: ${request.segments.size}, " +
            "waterSources: ${request.waterSources.size}, " +
            "campsites: ${request.campsites.size}, " +
            "supplies: ${request.supplies.size}, " +
            "markerPoints: ${request.markerPoints.size}"
        )
    }

    private fun deleteExistingPOIs(routeId: String) {
        val existingSegments = segmentRepository.findByRouteId(routeId)
        if (existingSegments.isNotEmpty()) {
            logger.info("删除路线 $routeId 的 ${existingSegments.size} 个旧分段")
            segmentRepository.deleteAll(existingSegments)
        }
        
        val deletedMarkerPoints = markerPointRepository.deleteByRouteId(routeId)
        if (deletedMarkerPoints > 0) {
            logger.info("删除路线 $routeId 的 $deletedMarkerPoints 个旧标记点")
        }
    }

    private fun saveSegments(routeId: String, segments: List<CallbackSegmentDto>) {
        segments.forEachIndexed { index, segDto ->
            val segment = createSegmentFromDto(routeId, segDto, index)
            segmentRepository.save(segment)
            logger.debug("保存分段: ${segment.id}, name: ${segment.name}")
        }
    }

    private fun createSegmentFromDto(
        routeId: String, 
        segDto: CallbackSegmentDto,
        index: Int
    ): Segment {
        val segmentId = if (segDto.id.startsWith("seg_")) {
            "seg_${idGenerator.generateShortId()}"
        } else {
            segDto.id
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
            routeType = mapSegmentTypeToRouteType(segDto.segmentType, segDto.slopeDirection),
            notes = buildNotesFromDto(segDto),
            startPointId = null,
            endPointId = null,
            sequenceNumber = segDto.sequenceNumber,
            trackStartIndex = segDto.trackStartIndex,
            trackEndIndex = segDto.trackEndIndex,
            color = segDto.color,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun mapSegmentTypeToRouteType(
        segmentType: String?,
        slopeDirection: String?
    ): Int? {
        return null
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

    private fun saveWaterSources(routeId: String, waterSources: List<CallbackWaterSourceDto>) {
        waterSources.forEachIndexed { index, wsDto ->
            val waterSource = createWaterSourceFromDto(routeId, wsDto, index)
            waterSourceRepository.save(waterSource)
            logger.debug("保存水源: ${waterSource.id}, name: ${waterSource.name}")
        }
    }

    private fun createWaterSourceFromDto(
        routeId: String,
        dto: CallbackWaterSourceDto,
        index: Int
    ): WaterSource {
        return WaterSource(
            id = "ws_${idGenerator.generateShortId()}",
            name = dto.name ?: "水源 ${index + 1}",
            description = dto.description,
            latitude = dto.latitude,
            longitude = dto.longitude,
            elevation = dto.elevation,
            waterType = mapWaterSourceType(dto.sourceType),
            waterQuality = 4,
            requiresTreatment = dto.sourceType == "river" || dto.sourceType == "spring",
            reliability = dto.reliability,
            notes = dto.notes,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            routeId = routeId
        )
    }

    private fun mapWaterSourceType(sourceType: String): Int {
        return when (sourceType.lowercase()) {
            "spring" -> WaterSourceType.NATURAL.value
            "river" -> WaterSourceType.NATURAL.value
            "tap" -> WaterSourceType.TREATED.value
            "bottled" -> WaterSourceType.BOTTLED.value
            else -> WaterSourceType.OTHER.value
        }
    }

    private fun saveCampsites(routeId: String, campsites: List<CallbackCampsiteDto>) {
        campsites.forEachIndexed { index, campDto ->
            val campsite = createCampsiteFromDto(routeId, campDto, index)
            campsiteRepository.save(campsite)
            logger.debug("保存营地: ${campsite.id}, name: ${campsite.name}")
        }
    }

    private fun createCampsiteFromDto(
        routeId: String,
        dto: CallbackCampsiteDto,
        index: Int
    ): Campsite {
        val notesParts = mutableListOf<String>()
        dto.hasWater?.let { notesParts.add("是否有水: $it") }
        dto.hasFacilities?.let { notesParts.add("是否有设施: $it") }
        dto.capacity?.let { notesParts.add("容量: $it") }
        dto.notes?.let { notesParts.add(it) }
        
        return Campsite(
            id = "camp_${idGenerator.generateShortId()}",
            routeId = routeId,
            name = dto.name ?: "营地 ${index + 1}",
            description = dto.description,
            latitude = dto.latitude,
            longitude = dto.longitude,
            elevation = dto.elevation,
            campsiteType = CampsiteType.OTHER.value,
            notes = if (notesParts.isNotEmpty()) notesParts.joinToString("; ") else null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun saveSupplies(routeId: String, supplies: List<CallbackSupplyDto>) {
        supplies.forEachIndexed { index, supplyDto ->
            val supply = createSupplyFromDto(routeId, supplyDto, index)
            supplyRepository.save(supply)
            logger.debug("保存补给点: ${supply.id}, name: ${supply.name}")
        }
    }

    private fun createSupplyFromDto(
        routeId: String,
        dto: CallbackSupplyDto,
        index: Int
    ): Supply {
        return Supply(
            id = "supply_${idGenerator.generateShortId()}",
            routeId = routeId,
            name = dto.name ?: "补给点 ${index + 1}",
            description = dto.description,
            latitude = dto.latitude?.let { BigDecimal.valueOf(it) },
            longitude = dto.longitude?.let { BigDecimal.valueOf(it) },
            elevation = dto.elevation?.let { BigDecimal.valueOf(it) },
            supplyType = mapSupplyType(dto.supplyType),
            notes = dto.notes,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun mapSupplyType(supplyType: String): Int {
        return when (supplyType.lowercase()) {
            "shop" -> 0
            "restaurant" -> 1
            "vending_machine" -> 2
            "emergency" -> 3
            else -> 4
        }
    }

    private fun saveMarkerPoints(routeId: String, markerPoints: List<CallbackMarkerPointDto>) {
        markerPoints.forEachIndexed { index, mpDto ->
            val markerPoint = createMarkerPointFromDto(routeId, mpDto, index)
            markerPointRepository.save(markerPoint)
            logger.debug("保存标记点: ${markerPoint.id}, name: ${markerPoint.name}")
        }
    }

    private fun createMarkerPointFromDto(
        routeId: String,
        dto: CallbackMarkerPointDto,
        index: Int
    ): MarkerPoint {
        return MarkerPoint(
            id = "mp_${idGenerator.generateShortId()}",
            routeId = routeId,
            name = dto.name ?: "标记点 ${index + 1}",
            description = dto.description,
            markerType = mapMarkerPointType(dto.type),
            iconUrl = dto.iconUrl,
            latitude = dto.latitude,
            longitude = dto.longitude,
            elevation = dto.elevation,
            color = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun mapMarkerPointType(type: String): Int {
        return when (type.lowercase()) {
            "scenic" -> MarkerPointType.SCENIC.value
            "viewpoint" -> MarkerPointType.VIEWPOINT.value
            "danger" -> MarkerPointType.DANGER.value
            "rest" -> MarkerPointType.REST.value
            "water" -> MarkerPointType.WATER.value
            "food" -> MarkerPointType.FOOD.value
            "shelter" -> MarkerPointType.SHELTER.value
            else -> MarkerPointType.OTHER.value
        }
    }

    fun triggerKmlAnalysis(routeId: String, kmlUrl: String): String {
        logger.info("触发 KML 分析，routeId: $routeId, kmlUrl: $kmlUrl")
        return "task_${idGenerator.generateShortId()}"
    }
}
