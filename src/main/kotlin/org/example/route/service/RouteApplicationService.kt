package org.example.route.service

import org.example.route.dto.RouteDetailResponse
import org.example.route.dto.RouteBasicResponse
import org.example.route.dto.toRoute
import org.example.route.model.Route
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * 路线应用服务
 * 协调多个领域服务，处理完整的业务用例
 */
@Service
class RouteApplicationService(
    private val routeService: RouteService
) {

    /**
     * 读取路线详情
     * 简单的读取操作，包含基本的业务逻辑
     */
    @Transactional(readOnly = true)
    fun getRouteDetails(routeId: String, userId: String? = null): RouteBasicResponse? {
        // 1. 获取路线详情
        val route = routeService.getRouteWithDetails(routeId) ?: return null

        // 2. 检查用户收藏状态（如果提供了用户ID）
        val isFavorited = userId?.let {
            routeService.isRouteFavorited(routeId, it)
        } ?: false

        // 3. 记录访问
        routeService.recordRouteVisit(routeId)

        // 4. 转换为DTO
        return RouteBasicResponse.fromRoute(route)
    }

    /**
     * 分页查询路线列表
     * 简单的搜索功能
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
        // 1. 搜索路线
        val routes = routeService.searchRoutes(
            keyword, regionId, difficulty, routeType,
            minDistance, maxDistance, null, userId, pageable
        )

        // 2. 转换为DTO
        return routes.map { RouteBasicResponse.fromRoute(it) }
    }

    /**
     * 获取路线完整详情
     * 包含所有关联对象信息
     */
    @Transactional(readOnly = true)
    fun getRouteWithFullDetails(routeId: String, userId: String? = null): RouteDetailResponse? {
        // 1. 获取路线详情（包含关联对象）
        val route = routeService.getRouteWithDetails(routeId) ?: return null

        // 2. 检查用户收藏状态
        val isFavorited = userId?.let {
            routeService.isRouteFavorited(routeId, it)
        } ?: false

        // 3. 记录访问
        routeService.recordRouteVisit(routeId)

        // 4. 转换为详细DTO
        return RouteDetailResponse.fromRoute(route, isFavorited)
    }

    /**
     * 写入路线
     * 简单的创建功能
     */
    @Transactional
    fun createRoute(
        name: String,
        description: String? = null,
        region: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        creatorId: String
    ): RouteBasicResponse {
        // 1. 创建路线实体
        val route = Route(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            region = region,
            difficulty = difficulty,
            routeType = routeType,
            createdBy = creatorId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 2. 保存路线
        val savedRoute = routeService.createRoute(route)

        // 3. 转换为响应DTO
        return RouteBasicResponse.fromRoute(savedRoute)
    }

    /**
     * 创建完整路线（包含关联对象）
     * 支持创建路线及其关联的路段、路点、标签、图片等
     * 使用分步保存策略确保外键约束正确
     */
    @Transactional
    fun createCompleteRoute(request: org.example.route.dto.RouteCreateRequest): RouteBasicResponse {
        // 步骤1: 创建并保存Route主实体
        val route = request.toRoute()
        val savedRoute = routeService.createRoute(route)

        // 步骤2: 创建简单关联对象（无外键依赖）
        createSimpleAssociations(savedRoute, request)

        // 步骤3: 保存Route和简单关联对象（包括Waypoint）
        val routeWithSimpleAssociations = routeService.updateRoute(savedRoute)

        // 步骤4: 创建复杂关联对象（依赖已持久化的Waypoint）
        createComplexAssociations(routeWithSimpleAssociations, request)

        // 步骤5: 最终保存所有关联对象
        val completeRoute = routeService.updateRoute(routeWithSimpleAssociations)

        // 步骤6: 重新加载完整数据并返回
        val finalRoute = routeService.getRouteWithDetails(completeRoute.id)
            ?: throw RuntimeException("创建路线后无法加载完整数据")

        return RouteBasicResponse.fromRoute(finalRoute)
    }

    /**
     * 创建简单关联对象（无外键依赖）
     */
    private fun createSimpleAssociations(route: Route, request: org.example.route.dto.RouteCreateRequest) {
        // 创建标签
        request.tags.forEach { tagName ->
            route.addTag(tagName)
        }

        // 创建图片
        request.images.forEach { imageRequest ->
            route.addImage(
                imageUrl = imageRequest.imageUrl,
                isCover = imageRequest.isCover,
                sequenceNumber = imageRequest.sequenceNumber
            )
        }

        // 创建路点（无外键依赖，只依赖Route）
        request.waypoints.forEach { waypointRequest ->
            val waypoint = org.example.route.model.Waypoint(
                id = waypointRequest.id ?: UUID.randomUUID().toString(),
                name = waypointRequest.name,
                description = waypointRequest.description,
                latitude = waypointRequest.latitude,
                longitude = waypointRequest.longitude,
                elevation = waypointRequest.elevation,
                type = waypointRequest.type,
                iconUrl = waypointRequest.iconUrl,
                imageUrl = waypointRequest.imageUrl,
                sequenceNumber = waypointRequest.sequenceNumber
            )
            // 设置关联关系
            waypoint.route = route
            route.waypoints.add(waypoint)
        }

        // 创建补给点
        request.supplies.forEach { supplyRequest ->
            val supply = org.example.route.model.Supply(
                id = supplyRequest.id ?: UUID.randomUUID().toString(),
                name = supplyRequest.name,
                description = supplyRequest.description,
                latitude = supplyRequest.latitude,
                longitude = supplyRequest.longitude,
                elevation = supplyRequest.elevation,
                supplyType = supplyRequest.supplyType,
                lastVerified = supplyRequest.lastVerified,
                updatedBy = supplyRequest.updatedBy,
                createdBy = route.createdBy
            )
            route.supplies.add(supply)
            supply.route = route
        }

        // 创建营地
        request.campsites.forEach { campsiteRequest ->
            val campsite = org.example.route.model.Campsite(
                id = campsiteRequest.id ?: UUID.randomUUID().toString(),
                name = campsiteRequest.name,
                description = campsiteRequest.description,
                latitude = campsiteRequest.latitude,
                longitude = campsiteRequest.longitude,
                elevation = campsiteRequest.elevation,
                campsiteType = campsiteRequest.campsiteType,
                notes = campsiteRequest.notes,
                createdBy = route.createdBy
            )
            route.campsites.add(campsite)
            campsite.route = route
        }

        // 创建标记点
        request.markerPoints.forEach { markerRequest ->
            val markerPoint = org.example.route.model.MarkerPoint(
                id = markerRequest.id ?: UUID.randomUUID().toString(),
                name = markerRequest.name,
                description = markerRequest.description,
                latitude = markerRequest.latitude,
                longitude = markerRequest.longitude,
                elevation = markerRequest.elevation,
                markerType = markerRequest.markerType,
                iconUrl = markerRequest.iconUrl
            )
            route.addMarkerPoint(markerPoint)
        }

        // 创建日程计划
        request.dailyPlans.forEach { planRequest ->
            val dailyPlan = org.example.route.model.DailyPlan(
                id = planRequest.id ?: UUID.randomUUID().toString(),
                title = planRequest.title,
                description = planRequest.description,
                dayNumber = planRequest.dayNumber,
                distance = planRequest.distance?.toDouble(),
                elevationGain = planRequest.elevationGain?.toInt(),
                elevationLoss = planRequest.elevationLoss?.toDouble(),
                estimatedTime = planRequest.estimatedTime,
                notes = planRequest.notes,
            )
            route.addDailyPlan(dailyPlan)
        }

        // 创建水源
        request.waterSources.forEach { waterSourceRequest ->
            val waterSource = org.example.water.model.WaterSource(
                id = waterSourceRequest.id ?: UUID.randomUUID().toString(),
                name = waterSourceRequest.name,
                description = waterSourceRequest.description,
                latitude = waterSourceRequest.latitude?.toDouble(),
                longitude = waterSourceRequest.longitude?.toDouble(),
                elevation = waterSourceRequest.elevation?.toDouble(),
                waterType = waterSourceRequest.waterType ?: 0,
                waterQuality = waterSourceRequest.waterQuality ?: 4,
                requiresTreatment = waterSourceRequest.requiresTreatment ?: false,
                reliability = waterSourceRequest.reliability,
                createdBy = route.createdBy
            )
            route.waterSources.add(waterSource)
            waterSource.route = route
        }

        // 创建搭车联系人
        request.hitchhikeContacts.forEach { hitchhikeRequest ->
            val hitchhikeContact = org.example.route.model.HitchhikeContact(
                id = hitchhikeRequest.id ?: UUID.randomUUID().toString(),
                name = hitchhikeRequest.name,
                phone = hitchhikeRequest.phone,
                description = hitchhikeRequest.description,
                location = hitchhikeRequest.location,
                price = hitchhikeRequest.price,
                lastVerified = hitchhikeRequest.verified ?: false,
                createdBy = route.createdBy
            )
            route.hitchhikeContacts.add(hitchhikeContact)
            hitchhikeContact.route = route
        }
    }

    /**
     * 创建复杂关联对象（依赖已持久化的Waypoint）
     */
    private fun createComplexAssociations(route: Route, request: org.example.route.dto.RouteCreateRequest) {
        // 此时Waypoint已经被持久化，可以安全地使用它们的ID
        val waypointMap = route.waypoints.associateBy { it.sequenceNumber }

        // 创建路段，使用已持久化的Waypoint ID
        request.segments.forEachIndexed { index, segmentRequest ->
            val segment = org.example.route.model.Segment(
                id = segmentRequest.id ?: UUID.randomUUID().toString(),
                name = "路段${index + 1}",
                description = "路段${index + 1}描述",
                distance = segmentRequest.distance,
                elevationGain = segmentRequest.elevationGain,
                elevationLoss = segmentRequest.elevationLoss,
                estimatedTime = segmentRequest.estimatedTime,
                difficulty = segmentRequest.difficulty,
                routeType = segmentRequest.terrain?.let {
                    when(it) {
                        "mountain" -> 1
                        "forest" -> 2
                        "desert" -> 3
                        else -> 0
                    }
                },
                // 使用对象引用而不是ID，让JPA自动管理外键关系
                startPoint = waypointMap[index + 1],
                endPoint = waypointMap[index + 2]
            )
            route.addSegment(segment)
        }
    }
}