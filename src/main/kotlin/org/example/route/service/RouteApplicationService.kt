package org.example.route.service

import org.example.route.dto.RouteBasicResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.dto.RouteUpdateRequest
import org.example.route.dto.toRoute
import org.example.route.model.Route
import org.example.route.model.Waypoint
import org.example.route.model.Segment
import org.example.route.dto.PoiPointDto
import org.example.route.dto.SegmentSchemeDto
import org.example.route.repository.*
import org.example.common.exception.BusinessException
import org.example.route.util.RouteQueryParamMapper
import org.example.common.util.IdGenerator
import org.example.trip.repository.TripRepository
import org.example.trip.repository.TripRouteAssociationRepository
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
    private val routeTagRepository: RouteTagRepository,
    private val dailyPlanRepository: DailyPlanRepository,
    private val hitchhikeContactRepository: HitchhikeContactRepository,
    private val routeImageRepository: RouteImageRepository,
    private val routeMapDataRepository: RouteMapDataRepository,
    private val routeRatingRepository: RouteRatingRepository,
    private val userRepository: UserRepository,
    private val segmentSchemeRepository: SegmentSchemeRepository,
    private val poiPointRepository: PoiPointRepository,
    private val routeRepository: org.example.route.repository.RouteRepository,
    private val tripRouteAssociationRepository: TripRouteAssociationRepository,
    private val tripRepository: TripRepository,
    private val objectMapper: ObjectMapper
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
     * 补充路线详情数据 - 从各个Repository查询关联数据
     */
    @Transactional(readOnly = true)
    private fun enrichRouteDetail(route: org.example.route.model.Route, userId: String?): org.example.route.dto.RouteDetailResponse {
        val tags = routeTagRepository.findByRouteId(route.id).map { it.tag }

        // 解析完整轨迹路径 JSON（分析回调写入）
        val trackPath: List<List<Double?>> = route.trackGeoJson?.let { json ->
            try {
                objectMapper.readValue(
                    json,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<List<Double?>>>() {}
                )
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        // 分段方案（每个方案包含内部分段列表）
        val segmentSchemes = run {
            val schemes = segmentSchemeRepository.findByRouteId(route.id)
            schemes.map { scheme ->
                val schemeSegments = segmentRepository.findByRouteId(route.id)
                    .filter { it.schemeId == scheme.id }
                    .map { org.example.route.dto.SegmentDto.fromSegment(it) }
                SegmentSchemeDto.fromScheme(scheme, schemeSegments)
            }
        }

        // 统一 POI 点
        val poiPoints = poiPointRepository.findByRouteId(route.id)
            .map { PoiPointDto.fromPoiPoint(it) }

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
            segmentSchemes = segmentSchemes,
            trackPath = trackPath,
            poiPoints = poiPoints,
            dailyPlans = dailyPlans,
            hitchhikeContacts = hitchhikeContacts,
            imageUrls = imageUrls,
            ratings = ratings,
            weatherInfo = null,
            trackPoints = trackPoints
        )
    }

    /**
     * 将 Route 实体转换为 RouteBasicResponse，补全 MapData/tags/ratings 关联数据
     * 注意：此方法会发起多次单条查询，适合列表已分页后的少量数据（≤20条）
     */
    @Transactional(readOnly = true)
    fun enrichRouteBasic(route: org.example.route.model.Route): RouteBasicResponse {
        val mapData = routeMapDataRepository.findById(route.id).orElse(null)
        val tags = routeTagRepository.findByRouteId(route.id).map { it.tag }
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
        return RouteBasicResponse(
            id = route.id,
            name = route.name,
            description = route.description,
            region = route.region,
            distance = mapData?.distance,
            duration = mapData?.duration,
            difficulty = route.difficulty,
            routeType = route.routeType,
            isLoop = route.isLoop,
            coverUrl = route.coverUrl,
            popularity = route.popularity,
            usageCount = route.usageCount,
            elevationGain = mapData?.elevationGain,
            elevationLoss = mapData?.elevationLoss,
            tags = tags,
            ratings = ratings,
            createdAt = route.createdAt.epochSecond,
            createdBy = route.createdBy,
            status = route.status,
            updatedAt = route.updatedAt.epochSecond
        )
    }

    /**
     * 业务用例：更新路线基本信息（管理端）
     *
     * 规则：
     * - 仅更新请求中出现的字段（null 表示不修改）
     * - 分析中（status=3）的路线不可编辑，避免与分析回调写入的数据竞争
     */
    @Transactional
    fun updateRouteBasic(routeId: String, request: RouteUpdateRequest): org.example.route.dto.RouteDetailResponse {
        val route = routeRepository.findById(routeId)
            .orElseThrow { BusinessException.notFound("路线不存在") }

        if (route.status == 3) {
            throw BusinessException.conflict("路线分析中，暂不可编辑，请等待分析完成或失败")
        }

        request.name?.takeIf { it.isNotBlank() }?.let { route.name = it.trim() }
        request.description?.let { route.description = it }
        request.region?.let { route.region = it }
        request.regionId?.let { route.regionId = it }
        request.difficulty?.let { route.difficulty = it }
        request.routeType?.let { route.routeType = it }
        request.isLoop?.let { route.isLoop = it }
        request.coverUrl?.let { route.coverUrl = it }
        route.updatedAt = Instant.now()
        routeRepository.save(route)

        // 返回更新后的完整详情（enrichRouteDetail 为私有方法，通过 getRouteFullDetails 走同一聚合逻辑）
        return getRouteFullDetails(routeId, null)
            ?: throw BusinessException.internalError("路线更新后详情加载失败")
    }

    /**
     * 业务用例：路线状态流转（管理端）
     *
     * 合法迁移矩阵（复用 Route 领域方法，非法迁移抛业务异常）：
     * - 0 → 1 publish（含发布前检查）
     * - 1 → 0 unpublish；1 → 2 close
     * - 2 → 0 reopen；2 → 1 reopen + publish
     * - 3 分析中 → 不可手动变更
     */
    @Transactional
    fun changeRouteStatus(routeId: String, targetStatus: Int, reason: String?): org.example.route.dto.RouteDetailResponse {
        val route = routeRepository.findById(routeId)
            .orElseThrow { BusinessException.notFound("路线不存在") }

        if (route.status == 3) {
            throw BusinessException.conflict("路线分析中，状态由分析回调自动流转，暂不可手动变更")
        }
        if (route.status == targetStatus) {
            throw BusinessException.badRequest("路线已处于目标状态")
        }

        when (targetStatus) {
            0 -> when (route.status) {
                1 -> route.unpublish()
                2 -> route.reopen()
                else -> throw BusinessException.badRequest("不支持的状态迁移: ${route.status} → $targetStatus")
            }

            1 -> {
                if (route.status == 2) {
                    route.reopen()
                }
                // 此时 status 必为 0，否则 publish() 领域校验会拦截
                if (route.status == 0) {
                    validatePublishReadiness(route)
                }
                route.publish()
            }

            2 -> if (route.status == 1) {
                route.close()
            } else {
                throw BusinessException.badRequest("不支持的状态迁移: ${route.status} → $targetStatus")
            }

            else -> throw BusinessException.badRequest("目标状态不合法: $targetStatus")
        }

        route.updatedAt = Instant.now()
        routeRepository.save(route)
        // reason 仅作运营记录，P1 引入审计表时落库
        return getRouteFullDetails(routeId, null)
            ?: throw BusinessException.internalError("路线状态流转后详情加载失败")
    }

    /**
     * 发布前检查：基础信息完整、轨迹数据已回填、无未采纳草稿
     */
    private fun validatePublishReadiness(route: Route) {
        if (route.name.isBlank()) {
            throw BusinessException.unprocessableEntity("发布前检查未通过：路线名称为空")
        }
        val mapData = routeMapDataRepository.findById(route.id).orElse(null)
        if (mapData?.distance == null) {
            throw BusinessException.unprocessableEntity(
                "发布前检查未通过：路线缺少轨迹距离数据，请先导入 KML 并完成分析"
            )
        }
        val draftSegments = segmentRepository.findByRouteId(route.id).count { it.status == "draft" }
        if (draftSegments > 0) {
            throw BusinessException.unprocessableEntity(
                "发布前检查未通过：仍有 $draftSegments 个分段草稿未采纳，请先在路线工作台处理"
            )
        }
        val draftPois = poiPointRepository.findByRouteId(route.id).count { it.status == "draft" }
        if (draftPois > 0) {
            throw BusinessException.unprocessableEntity(
                "发布前检查未通过：仍有 $draftPois 个 POI 草稿未采纳，请先在路线工作台处理"
            )
        }
    }

    /**
     * 业务用例：删除路线（管理端，软删除）
     *
     * 规则：
     * - 分析中（status=3）不可删除
     * - 被非已取消行程引用时默认拒绝，force=true 可强制软删
     * - 已入全局 POI 库的条目不受影响（全局资产，仅来源路线指向已删路线）
     */
    @Transactional
    fun deleteRoute(routeId: String, force: Boolean) {
        val route = routeRepository.findById(routeId)
            .orElseThrow { BusinessException.notFound("路线不存在") }

        if (route.status == 3) {
            throw BusinessException.conflict("路线分析进行中，请等待分析完成或失败后再删除")
        }

        val associations = tripRouteAssociationRepository.findByRouteId(routeId)
        if (associations.isNotEmpty()) {
            val tripIds = associations.map { it.tripId }
            val activeTrips = tripRepository.findAllById(tripIds).filter { it.status != 3 }
            if (activeTrips.isNotEmpty() && !force) {
                throw BusinessException.conflict(
                    "路线被 ${activeTrips.size} 个未取消的行程引用（如：${activeTrips.first().name}），删除将影响这些行程；如确认删除请使用强制删除",
                    details = mapOf("referenced_trip_ids" to activeTrips.map { it.id })
                )
            }
        }

        route.isDeleted = true
        route.updatedAt = Instant.now()
        routeRepository.save(route)
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

        // 2. DTO转换（应用层职责）- 带关联数据补全
        return routes.map { enrichRouteBasic(it) }
    }
    
    /**
     * 业务用例：管理端路线搜索（全状态，可按状态筛选）
     * 仅限后台使用；C 端一律走 searchRoutes（只返回已发布）
     */
    fun searchRoutesForAdmin(
        keyword: String? = null,
        difficulty: Int? = null,
        status: Int? = null,
        pageable: Pageable
    ): Page<RouteBasicResponse> {
        val routes = routeRepository.searchRoutes(
            keyword = keyword,
            region = null,
            difficulty = difficulty,
            routeType = null,
            status = status,
            pageable = pageable
        )
        return routes.map { enrichRouteBasic(it) }
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
        return enrichRouteBasic(savedRoute)
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
        return routes.map { enrichRouteBasic(it) }
    }

    /**
     * 业务用例：获取新晋路线
     * 通过领域服务获取按创建时间排序的路线
     */
    @Transactional(readOnly = true)
    fun getNewRoutes(limit: Int): Page<RouteBasicResponse> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, limit)
        val routes = routeService.getNewRoutes(limit, pageable)
        return routes.map { enrichRouteBasic(it) }
    }

    /**
     * 业务用例：获取季节性路线
     * 通过领域服务获取对应季节的路线
     */
    @Transactional(readOnly = true)
    fun getSeasonalRoutes(season: String?, limit: Int): Page<RouteBasicResponse> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, limit)
        val routes = routeService.getSeasonalRoutes(season, limit, pageable)
        return routes.map { enrichRouteBasic(it) }
    }

    /**
     * 业务用例：获取周末路线
     * 通过领域服务获取适合周末的路线
     */
    @Transactional(readOnly = true)
    fun getWeekendRoutes(limit: Int): Page<RouteBasicResponse> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, limit)
        val routes = routeService.getWeekendRoutes(limit, pageable)
        return routes.map { enrichRouteBasic(it) }
    }

    /**
     * 业务用例：统一搜索路线（支持所有抽象参数）
     * 通过领域服务进行统一搜索，支持 category、tags 等抽象参数
     */
    @Transactional(readOnly = true)
    fun searchRoutesUnified(
        keyword: String? = null,
        category: String? = null,
        tags: String? = null,
        regionId: String? = null,
        difficulty: String? = null,
        routeType: String? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        userId: String? = null,
        sort: String? = "popular",
        pageable: Pageable
    ): Page<RouteBasicResponse> {
        // 1. 解析抽象参数
        // category 映射到对应的标签列表
        val categoryTags = RouteQueryParamMapper.getTagsForCategory(category)
        
        // 解析 tags 参数（逗号分隔）
        val parsedTags = RouteQueryParamMapper.parseTags(tags)
        
        // 合并 category 和 tags
        val allTags = (categoryTags + parsedTags).distinct()
        
        // 解析 difficulty（支持字符串和数字）
        val parsedDifficulty = if (difficulty != null) {
            RouteQueryParamMapper.parseDifficulty(difficulty)
        } else {
            null
        }
        
        // 解析 routeType（支持字符串和数字）
        val parsedRouteType = if (routeType != null) {
            RouteQueryParamMapper.parseRouteType(routeType)
        } else {
            null
        }

        // 2. 通过领域服务进行统一搜索
        val routes = routeService.searchRoutesUnified(
            keyword = keyword,
            regionId = regionId,
            difficulty = parsedDifficulty,
            routeType = parsedRouteType,
            minDistance = minDistance,
            maxDistance = maxDistance,
            userId = userId,
            tags = if (allTags.isNotEmpty()) allTags else null,
            sortBy = sort,
            pageable = pageable
        )

        // 3. DTO转换（应用层职责）
        return routes.map { enrichRouteBasic(it) }
    }

    /**
     * 业务用例：按类别获取路线
     * 这是一个简化的方法，专门用于前端过滤器快速查询
     */
    @Transactional(readOnly = true)
    fun getRoutesByCategory(
        category: String,
        limit: Int
    ): Page<RouteBasicResponse> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, limit)
        return searchRoutesUnified(
            category = category,
            sort = "popular",
            pageable = pageable
        )
    }
}