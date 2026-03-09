package org.example.route.service

import org.example.route.dto.RouteBasicResponse
import org.example.route.dto.toRoute
import org.example.route.model.Route
import org.example.route.model.Waypoint
import org.example.route.model.Segment
import org.example.route.repository.*
import org.example.common.util.IdGenerator
import org.example.water.repository.WaterSourceRepository
import org.example.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 路线应用服务（重构版 - 单向关联）
 * 职责：业务用例编排、跨领域协调、DTO转换
 * 严格遵循分层架构，所有数据访问都通过DomainService或Repository
 */
@Service
class RouteApplicationService(
    private val routeService: RouteService,
    private val waypointRepository: WaypointRepository,
    private val segmentRepository: SegmentRepository,
    // Phase 1: 添加关联数据Repository注入
    private val routeTagRepository: RouteTagRepository,
    private val campsiteRepository: CampsiteRepository,
    private val supplyRepository: SupplyRepository,
    private val waterSourceRepository: WaterSourceRepository,
    private val markerPointRepository: MarkerPointRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val hitchhikeContactRepository: HitchhikeContactRepository,
    private val routeImageRepository: RouteImageRepository,
    private val routeMapDataRepository: RouteMapDataRepository,
    private val routeRatingRepository: RouteRatingRepository,
    private val userRepository: UserRepository
) {

    /**
     * 业务用例：获取路线完整详情（包含所有关联信息）
     * 通过领域服务协调业务逻辑和数据访问
     */
    @Transactional(readOnly = true)
    fun getRouteFullDetails(routeId: String, userId: String? = null): org.example.route.dto.RouteDetailResponse? {
        // 1. 通过领域服务获取路线详情（包含业务规则检查）
        val route = routeService.getRouteWithAccessCheck(routeId, userId) ?: return null

        // 2. 业务逻辑协调：记录访问
        routeService.recordRouteVisitIfNeeded(route, userId)

        // 3. 检查用户收藏状态
        val isFavorite = userId?.let { routeService.isRouteFavorited(routeId, it) } ?: false

        // 4. DTO转换（应用层职责）使用enrichRouteDetail方法来填充所有关联数据
        return enrichRouteDetail(route, userId).copy(isFavorite = isFavorite)
    }

    /**
     * Phase 1: 补充路线详情数据 - 从各个Repository查询关联数据
     * 确保所有嵌套集合都被填充而不是返回空列表
     */
    @Transactional(readOnly = true)
    private fun enrichRouteDetail(route: org.example.route.model.Route, userId: String?): org.example.route.dto.RouteDetailResponse {
        // 批量查询所有关联数据
        val tags = routeTagRepository.findByRouteId(route.id).map { it.tag }
        val segments = segmentRepository.findByRouteId(route.id)
            .map { org.example.route.dto.SegmentDto.fromSegment(it) }
        val campsites = campsiteRepository.findByRouteId(route.id, org.springframework.data.domain.PageRequest.of(0, Int.MAX_VALUE)).content
            .map { org.example.route.dto.CampsiteDto.fromCampsite(it) }
        val supplies = supplyRepository.findByRouteId(route.id, org.springframework.data.domain.PageRequest.of(0, Int.MAX_VALUE)).content
            .map { org.example.route.dto.SupplyDto.fromSupply(it) }
        val waterSources = waterSourceRepository.findByRouteId(route.id, org.springframework.data.domain.PageRequest.of(0, Int.MAX_VALUE)).content
            .map { org.example.water.dto.WaterSourceDto.fromWaterSource(it) }
        val markerPoints = markerPointRepository.findByRouteId(route.id)
            .map { org.example.route.dto.MarkerPointDto.fromMarkerPoint(it) }
        val dailyPlans = dailyPlanRepository.findByRouteId(route.id)
            .map { org.example.route.dto.DailyPlanDto.fromDailyPlan(it) }
        val hitchhikeContacts = hitchhikeContactRepository.findByRouteId(route.id)
            .map { org.example.route.dto.HitchhikeContactDto.fromHitchhikeContact(it) }
        
        // 查询创建者信息
        val creator = route.createdBy?.let { createdById ->
            userRepository.findById(createdById).orElse(null)?.let { user ->
                org.example.user.dto.UserBasicDto(
                    id = user.id,
                    username = user.username,
                    nickname = user.nickname ?: user.username,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    createdAt = user.createdAt.epochSecond
                )
            }
        }
        
        // 查询图片URL
        val imageUrls = routeImageRepository.findByRouteIdOrderBySequenceNumber(route.id)
            .mapNotNull { it.imageUrl }
        
        // 查询地图数据（距离、时长、海拔等）
        val mapData = routeMapDataRepository.findById(route.id).orElse(null)
        val distance = mapData?.distance?.toDouble()
        val duration = mapData?.duration
        val elevationGain = mapData?.elevationGain?.toDouble()
        val elevationLoss = mapData?.elevationLoss?.toDouble()
        
        // 查询评分数据
        val ratingData = routeRatingRepository.findByRouteId(route.id)
        val ratings = ratingData?.let {
            org.example.route.dto.RatingDto(
                overall = it.overall,
                scenery = it.scenery,
                difficulty = it.difficulty,
                experience = it.experience,
                facilities = it.facilities,
                ratingCount = it.ratingCount
            )
        }
        
        // 查询轨迹点数据（按序列号排序）
        val trackPoints = waypointRepository.findByRouteIdOrderBySequenceNumberAsc(route.id)
            .map { org.example.route.dto.TrackPointDto.fromWaypoint(it) }
        
        // 查询 KML/GPX URL
        val kmlUrl = mapData?.kmlUrl
        val gpxUrl = mapData?.gpxUrl
        
        // 构建完整的RouteDetailResponse
        return org.example.route.dto.RouteDetailResponse(
            id = route.id,
            name = route.name,
            description = route.description,
            regionId = route.regionId,
            region = route.region,
            distance = distance,
            duration = duration,
            elevationGain = elevationGain,
            elevationLoss = elevationLoss,
            difficulty = route.difficulty,
            routeType = route.routeType,
            routeDirection = null,
            coverUrl = route.coverUrl,
            defaultMapId = route.defaultMapId,
            kmlUrl = kmlUrl,
            gpxUrl = gpxUrl,
            popularity = route.popularity,
            usageCount = route.usageCount,
            isLoop = route.isLoop,
            isFavorite = false,  // 将由调用方覆盖
            status = route.status,
            createdAt = route.createdAt.epochSecond,
            updatedAt = route.updatedAt.epochSecond,
            createdBy = route.createdBy,
            creator = creator,  // 已填充创建者信息
            
            // 关联数据
            tags = tags,
            segments = segments,
            campsites = campsites,
            supplies = supplies,
            waterSources = waterSources,
            markerPoints = markerPoints,
            dailyPlans = dailyPlans,
            hitchhikeContacts = hitchhikeContacts,
            imageUrls = imageUrls,
            ratings = ratings,
            weatherInfo = null,  // 暂时设为null，可后续集成天气API
            trackPoints = trackPoints
        )
    }

    /**
     * 业务用例：分页搜索路线
     * 通过领域服务进行搜索，遵循分层架构
     */
    @Transactional(readOnly = true)
    fun searchRoutes(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        userId: String? = null,
        pageable: Pageable
    ): Page<RouteBasicResponse> {
        // 1. 通过领域服务进行搜索（包含业务规则）
        val routes = routeService.searchRoutes(
            keyword = keyword,
            regionId = regionId,
            difficulty = difficulty,
            routeType = routeType,
            minDistance = minDistance,
            maxDistance = maxDistance,
            userId = userId,
            pageable = pageable
        )

        // 2. DTO转换（应用层职责）
        return routes.map { RouteBasicResponse.fromRoute(it) }
    }
    
    /**
     * 业务用例：创建完整路线（重构版 - 单向关联）
     * 使用单向关联的方式创建路线及其关联对象
     */
    @Transactional
    fun createCompleteRoute(request: org.example.route.dto.RouteCreateRequest): RouteBasicResponse {
        // 1. 业务规则验证（通过领域服务）
        routeService.validateCompleteRouteCreation(request)

        // 2. 创建路线主体
        val route = Route(
            id = IdGenerator.generateIdWithPrefix("route"),
            name = request.name,
            description = request.description,
            region = request.region,
            regionId = request.regionId,
            difficulty = request.difficulty,
            routeType = request.routeType,
            status = 0, // 规划中
            coverUrl = request.coverUrl,
            defaultMapId = request.defaultMapId,
            createdBy = request.createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 3. 保存路线主体
        val savedRoute = routeService.createRouteWithValidation(route)

        // 4. 创建路点（单向关联 - 只存 routeId）
        val savedWaypoints = mutableMapOf<Int, Waypoint>()
        request.waypoints.forEach { waypointRequest ->
            val waypoint = Waypoint(
                id = IdGenerator.generateIdWithPrefix("waypoint"),
                routeId = savedRoute.id,  // 单向关联
                name = waypointRequest.name,
                description = waypointRequest.description,
                latitude = waypointRequest.latitude,
                longitude = waypointRequest.longitude,
                elevation = waypointRequest.elevation,
                type = waypointRequest.type,
                iconUrl = waypointRequest.iconUrl,
                imageUrl = waypointRequest.imageUrl,
                sequenceNumber = waypointRequest.sequenceNumber,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            val saved = waypointRepository.save(waypoint)
            savedWaypoints[saved.sequenceNumber] = saved
        }

        // 5. 创建路段（单向关联 - 只存 routeId 和 waypointId）
        request.segments.forEachIndexed { index, segmentRequest ->
            val segment = Segment(
                id = IdGenerator.generateIdWithPrefix("segment"),
                routeId = savedRoute.id,  // 单向关联
                name = segmentRequest.name ?: "路段${index + 1}",
                description = segmentRequest.description,
                distance = segmentRequest.distance,
                elevationGain = segmentRequest.elevationGain,
                elevationLoss = segmentRequest.elevationLoss,
                estimatedTime = segmentRequest.estimatedTime,
                difficulty = segmentRequest.difficulty,
                routeType = segmentRequest.routeType,
                notes = segmentRequest.notes,
                startPointId = savedWaypoints[segmentRequest.startSequence]?.id,  // 单向关联
                endPointId = savedWaypoints[segmentRequest.endSequence]?.id,  // 单向关联
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            segmentRepository.save(segment)
        }

        // 6. TODO: 创建其他关联对象（tags、images、campsites、supplies等）
        // 这些可以后续添加，使用相同的单向关联模式

        // 7. DTO转换（应用层职责）
        return RouteBasicResponse.fromRoute(savedRoute)
    }

    /**
     * 业务用例：获取热门路线
     * 通过领域服务获取按热度排序的路线
     */
    @Transactional(readOnly = true)
    fun getPopularRoutes(limit: Int): Page<RouteBasicResponse> {
        // 1. 通过领域服务获取热门路线
        val pageable = org.springframework.data.domain.PageRequest.of(0, limit)
        val routes = routeService.getPopularRoutes(limit, pageable)

        // 2. DTO转换（应用层职责）
        return routes.map { RouteBasicResponse.fromRoute(it) }
    }
}