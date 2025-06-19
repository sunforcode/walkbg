package org.example.route.service

import org.example.route.dto.RouteDetailResponse
import org.example.route.dto.RouteBasicResponse
import org.example.route.dto.toRoute
import org.example.route.model.Route
import org.example.common.util.IdGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 路线应用服务
 * 职责：业务用例编排、跨领域协调、DTO转换
 * 严格遵循分层架构，所有数据访问都通过DomainService
 */
@Service
class RouteApplicationService(
    private val routeService: RouteService
) {

    /**
     * 业务用例：获取路线基础详情
     * 通过领域服务协调业务逻辑和数据访问
     */
    @Transactional(readOnly = true)
    fun getRouteDetails(routeId: String, userId: String? = null): RouteBasicResponse? {
        // 1. 通过领域服务获取路线详情（包含业务规则检查）
        val route = routeService.getRouteWithAccessCheck(routeId, userId) ?: return null

        // 2. 业务逻辑协调：检查收藏状态和记录访问
        val isFavorited = userId?.let { routeService.isRouteFavorited(routeId, it) } ?: false
        routeService.recordRouteVisitIfNeeded(route, userId)

        // 3. DTO转换（应用层职责）
        return RouteBasicResponse.fromRoute(route)
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
     * 业务用例：获取路线完整详情
     * 复杂业务用例，通过领域服务协调多个操作
     */
    @Transactional(readOnly = true)
    fun getRouteWithFullDetails(routeId: String, userId: String? = null): RouteDetailResponse? {
        // 1. 通过领域服务获取路线详情（包含关联对象和业务规则检查）
        val route = routeService.getRouteWithFullDetailsAndAccessCheck(routeId, userId) ?: return null

        // 2. 业务逻辑协调（通过领域服务）
        val isFavorited = userId?.let { routeService.isRouteFavorited(routeId, it) } ?: false
        routeService.recordRouteVisitIfNeeded(route, userId)

        // 3. DTO转换（应用层职责）
        return RouteDetailResponse.fromRoute(route, isFavorited)
    }

    /**
     * 业务用例：创建简单路线
     * 简单创建操作，基本业务逻辑委托给DomainService
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
        // 1. 构建路线实体
        val route = Route(
            id = IdGenerator.generateIdWithPrefix("route"),
            name = name,
            description = description,
            region = region,
            difficulty = difficulty,
            routeType = routeType,
            createdBy = creatorId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // 2. 业务规则验证和创建（通过领域服务）
        val savedRoute = routeService.createRouteWithValidation(route)

        // 3. DTO转换（应用层职责）
        return RouteBasicResponse.fromRoute(savedRoute)
    }

    /**
     * 业务用例：创建完整路线
     * 复杂业务用例，通过领域服务协调多个步骤和业务规则
     */
    @Transactional
    fun createCompleteRoute(request: org.example.route.dto.RouteCreateRequest): RouteBasicResponse {
        // 1. 业务规则验证（通过领域服务）
        routeService.validateCompleteRouteCreation(request)

        // 2. 构建完整路线实体（包含所有关联对象）
        val route = buildCompleteRoute(request)

        // 3. 通过领域服务创建完整路线（包含所有业务规则和数据持久化）
        val savedRoute = routeService.createRouteWithValidation(route)

        // 4. DTO转换（应用层职责）
        return RouteBasicResponse.fromRoute(savedRoute)
    }

    /**
     * 构建完整路线实体（包含所有关联对象）
     * 应用层职责：实体构建和组装
     */
    private fun buildCompleteRoute(request: org.example.route.dto.RouteCreateRequest): Route {
        // 1. 构建路线主体
        val route = request.toRoute().copy(id = IdGenerator.generateIdWithPrefix("route"))

        // 2. 创建简单关联对象（无外键依赖）
        createSimpleAssociations(route, request)

        // 3. 创建复杂关联对象（依赖已构建的关联对象）
        createComplexAssociations(route, request)

        return route
    }

    /**
     * 创建简单关联对象（无外键依赖）
     * 内部实体ID自动生成
     */
    private fun createSimpleAssociations(route: Route, request: org.example.route.dto.RouteCreateRequest) {
        // 创建标签（自动生成ID）
        request.tags.forEach { tagName ->
            route.addTag(tagName)
        }

        // 创建图片（自动生成ID）
        request.images.forEach { imageRequest ->
            route.addImage(
                imageUrl = imageRequest.imageUrl,
                isCover = imageRequest.isCover,
                sequenceNumber = imageRequest.sequenceNumber
            )
        }

        // 创建路点（自动生成ID）
        request.waypoints.forEach { waypointRequest ->
            val waypoint = org.example.route.model.Waypoint(
                id = IdGenerator.generateIdWithPrefix("waypoint"),
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

        // 创建补给点（自动生成ID）
        request.supplies.forEach { supplyRequest ->
            val supply = org.example.route.model.Supply(
                id = IdGenerator.generateIdWithPrefix("supply"),
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

        // 创建营地（自动生成ID）
        request.campsites.forEach { campsiteRequest ->
            val campsite = org.example.route.model.Campsite(
                id = IdGenerator.generateIdWithPrefix("campsite"),
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

        // 创建标记点（自动生成ID）
        request.markerPoints.forEach { markerRequest ->
            val markerPoint = org.example.route.model.MarkerPoint(
                id = IdGenerator.generateIdWithPrefix("marker"),
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

        // 创建日程计划（自动生成ID）
        request.dailyPlans.forEach { planRequest ->
            val dailyPlan = org.example.route.model.DailyPlan(
                id = IdGenerator.generateIdWithPrefix("plan"),
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

        // 创建水源（自动生成ID）
        request.waterSources.forEach{ waterSourceRequest ->
            val waterSource = org.example.water.model.WaterSource(
                id = IdGenerator.generateIdWithPrefix("water"),
                name = waterSourceRequest.name,
                description = waterSourceRequest.description,
                latitude = waterSourceRequest.latitude?.toDouble(),
                longitude = waterSourceRequest.longitude?.toDouble(),
                elevation = waterSourceRequest.elevation?.toDouble(),
                waterType = waterSourceRequest.waterType ?: 0,
                waterQuality = waterSourceRequest.waterQuality ?: 4,
                requiresTreatment = waterSourceRequest.requiresTreatment,
                reliability = waterSourceRequest.reliability
            )
            route.waterSources.add(waterSource)
            waterSource.route = route
            // 通过关联对象设置创建者（如果需要的话，可以在这里设置）
            waterSource.creator = route.creator
        }

        // 创建搭车联系人（自动生成ID）
        request.hitchhikeContacts.forEach { hitchhikeRequest ->
            val hitchhikeContact = org.example.route.model.HitchhikeContact(
                id = IdGenerator.generateIdWithPrefix("contact"),
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
     * 创建复杂关联对象（依赖已构建的关联对象）
     */
    private fun createComplexAssociations(route: Route, request: org.example.route.dto.RouteCreateRequest) {
        // 此时Waypoint已经被构建，可以安全地使用它们的序号
        val waypointMap = route.waypoints.associateBy { it.sequenceNumber }

        // 创建路段，使用已构建的Waypoint（自动生成路段ID）
        request.segments.forEachIndexed { index, segmentRequest ->
            val segment = org.example.route.model.Segment(
                id = IdGenerator.generateIdWithPrefix("segment"),
                name = "路段${index + 1}",
                description = "路段${index + 1}描述",
                distance = segmentRequest.distance,
                elevationGain = segmentRequest.elevationGain,
                elevationLoss = segmentRequest.elevationLoss,
                estimatedTime = segmentRequest.estimatedTime,
                difficulty = segmentRequest.difficulty,
                routeType = null,
                notes = null,
                // 使用对象引用而不是ID，让JPA自动管理外键关系
                startPoint = waypointMap[index + 1],
                endPoint = waypointMap[index + 2]
            )
            route.addSegment(segment)
        }
    }
}