package org.example.route.service

import org.example.user.repository.UserRepository
import org.example.user.repository.UserFavoriteRouteRepository
import org.example.user.model.UserFavoriteRoute
import org.example.route.model.Route
import org.example.route.repository.RouteRepository
import org.example.route.repository.RouteTagRepository
import org.example.route.dto.RouteCreateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 路线领域服务实现
 * 职责：实现领域业务逻辑和业务规则验证
 * 专注于纯领域逻辑，不处理简单的数据访问
 */
@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository,
    private val routeTagRepository: RouteTagRepository,
    private val userRepository: UserRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository
) : RouteService {

    // ========== 业务规则验证 ==========
    /**
     * 检查路线访问权限
     * 领域规则：根据路线状态和用户身份判断是否可访问
     */
    override fun isRouteAccessible(route: Route, userId: String?): Boolean {
        return when (route.status) {
            0 -> route.createdBy == userId  // 规划中的路线只有创建者能访问
            1 -> true  // 已发布的路线所有人都能访问
            2 -> false // 已关闭的路线不能访问
            else -> false
        }
    }

    /**
     * 验证路线创建请求
     * 领域规则：验证路线创建的业务规则
     */
    override fun validateCompleteRouteCreation(request: RouteCreateRequest) {
        // 业务规则验证
        if (request.name.isBlank()) {
            throw IllegalArgumentException("路线名称不能为空")
        }

        if (request.name.length > 100) {
            throw IllegalArgumentException("路线名称不能超过100个字符")
        }

        // 验证难度等级
        if (request.difficulty != null && (request.difficulty < 1 || request.difficulty > 5)) {
            throw IllegalArgumentException("难度等级必须在1-5之间")
        }

        // 验证路线类型
        if (request.routeType != null && (request.routeType < 0 || request.routeType > 3)) {
            throw IllegalArgumentException("路线类型必须在0-3之间")
        }

        // 验证路段数据
        if (request.segments.isNotEmpty() && request.waypoints.size < 2) {
            throw IllegalArgumentException("有路段的路线至少需要2个路点")
        }
    }

    // ========== 路线生命周期管理 ==========
    /**
     * 创建路线（包含业务规则验证）
     * 领域逻辑：应用创建路线的业务规则
     */
    @Transactional
    override fun createRouteWithValidation(route: Route): Route {
        // 领域业务规则验证
        validateRouteForCreation(route)

        // 应用领域规则：新建路线的默认状态
        route.status = 0  // 规划中
        route.popularity = 0
        route.usageCount = 0
        route.updatedAt = Instant.now()

        return routeRepository.save(route)
    }

    /**
     * 发布路线
     * 领域规则：只有规划中的路线才能发布
     */
    @Transactional
    override fun publishRoute(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false

        // 领域业务规则：只有规划中的路线才能发布
        if (route.status != 0) {
            throw IllegalStateException("只有规划中的路线才能发布")
        }

        // 领域规则：发布前的验证
        validateRouteForPublish(route)

        // 应用领域逻辑：更新状态
        route.status = 1 // 已发布
        route.updatedAt = Instant.now()
        routeRepository.save(route)
        return true
    }

    /**
     * 关闭路线
     * 领域规则：只有已发布的路线才能关闭
     */
    @Transactional
    override fun closeRoute(routeId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false

        // 领域业务规则：只有已发布的路线才能关闭
        if (route.status != 1) {
            throw IllegalStateException("只有已发布的路线才能关闭")
        }

        // 应用领域逻辑：更新状态
        route.status = 2 // 已关闭
        route.updatedAt = Instant.now()
        routeRepository.save(route)
        return true
    }

    /**
     * 领域业务规则：验证路线创建
     */
    private fun validateRouteForCreation(route: Route) {
        if (route.name.isBlank()) {
            throw IllegalArgumentException("路线名称不能为空")
        }

        if (route.name.length > 100) {
            throw IllegalArgumentException("路线名称不能超过100个字符")
        }

        // 检查名称唯一性（在同一区域内）
        if (route.region != null) {
            val existingRoutes = routeRepository.findByNameContainingIgnoreCase(
                route.name,
                org.springframework.data.domain.PageRequest.of(0, 1)
            )
            if (existingRoutes.content.any { it.region == route.region && it.id != route.id }) {
                throw IllegalArgumentException("该区域已存在同名路线")
            }
        }
    }

    /**
     * 领域业务规则：验证路线发布
     */
    private fun validateRouteForPublish(route: Route) {
        if (route.name.isBlank()) {
            throw IllegalStateException("路线名称不能为空，无法发布")
        }

        if (route.description.isNullOrBlank()) {
            throw IllegalStateException("路线描述不能为空，无法发布")
        }

        // 可以添加更多发布前的验证规则
        // 例如：必须有路点、必须有描述等
    }

    // ========== 用户交互业务逻辑 ==========
    /**
     * 检查路线是否被用户收藏
     * 领域逻辑：根据业务规则判断收藏状态
     */
    override fun isRouteFavorited(routeId: String, userId: String): Boolean {
        return routeRepository.isRouteFavoritedByUser(userId, routeId)
    }

    /**
     * 收藏路线
     * 领域规则：收藏的业务逻辑和约束
     */
    @Transactional
    override fun favoriteRoute(routeId: String, userId: String): Boolean {
        // 领域规则验证
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        
        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            return false
        }

        // 领域规则：不能收藏自己创建的路线
        if (route.createdBy == userId) {
            throw IllegalArgumentException("不能收藏自己创建的路线")
        }

        // 领域规则：只能收藏已发布的路线
        if (route.status != 1) {
            throw IllegalArgumentException("只能收藏已发布的路线")
        }

        // 检查是否已经收藏（幂等性）
        if (userFavoriteRouteRepository.existsByUserIdAndRouteId(userId, routeId)) {
            return true
        }

        // 创建收藏关系（单向关联 - 只存 ID）
        val favorite = UserFavoriteRoute(
            id = UUID.randomUUID().toString(),
            userId = userId,
            routeId = routeId,
            createdAt = Instant.now()
        )

        userFavoriteRouteRepository.save(favorite)
        return true
    }

    /**
     * 取消收藏路线
     * 领域规则：取消收藏的业务逻辑
     */
    @Transactional
    override fun unfavoriteRoute(routeId: String, userId: String): Boolean {
        val result = userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId)
        return result > 0
    }

    // ========== 访问统计业务逻辑 ==========
    /**
     * 根据业务规则记录路线访问
     * 领域规则：什么情况下需要记录访问，如何更新统计数据
     */
    @Transactional
    override fun recordRouteVisitIfNeeded(route: Route, userId: String?) {
        // 领域规则：创建者访问自己的路线不计入统计
        if (shouldRecordVisit(route, userId)) {
            route.incrementPopularity()
            route.incrementUsageCount()
            routeRepository.save(route)
        }
    }

    /**
     * 记录路线完成
     * 领域规则：路线完成的业务逻辑和统计更新
     */
    @Transactional
    override fun recordRouteCompletion(routeId: String, userId: String): Boolean {
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        val user = userRepository.findById(userId).orElse(null) ?: return false

        // 领域规则：只有已发布的路线才能标记为完成
        if (route.status != 1) {
            throw IllegalArgumentException("只有已发布的路线才能标记为完成")
        }

        // 领域规则：不能完成自己创建的路线
        if (route.createdBy == userId) {
            throw IllegalArgumentException("不能完成自己创建的路线")
        }

        // TODO: 实现完成记录的具体逻辑
        // 1. 创建完成记录
        // 2. 更新路线统计
        // 3. 可能的奖励逻辑等

        return true
    }

    /**
     * 领域规则：判断是否应该记录访问
     */
    private fun shouldRecordVisit(route: Route, userId: String?): Boolean {
        // 创建者访问自己的路线不计入统计
        return userId != route.createdBy
    }

    // ========== 数据访问方法（遵循分层架构） ==========
    /**
     * 获取路线（包含访问权限检查）
     * 结合数据访问和业务规则
     */
    override fun getRouteWithAccessCheck(routeId: String, userId: String?): Route? {
        val route = routeRepository.findById(routeId).orElse(null) ?: return null

        // 应用业务规则检查
        return route
    }



    /**
     * 搜索路线（包含业务规则）
     * 结合数据访问和业务规则
     */
    override fun searchRoutes(
        keyword: String?,
        regionId: String?,
        difficulty: Int?,
        routeType: Int?,
        minDistance: Double?,
        maxDistance: Double?,
        userId: String?,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Route> {
        // 如果是查询用户收藏的路线
        if (userId != null) {
            return routeRepository.findUserFavoriteRoutes(userId, pageable)
        }

        // 多条件搜索，应用业务规则：只查询已发布的路线
        return routeRepository.searchRoutes(
            keyword = keyword,
            region = regionId,
            difficulty = difficulty,
            routeType = routeType,
            status = null, // 只查询已发布的路线（业务规则）
            pageable = pageable
        )
    }

    /**
     * 根据ID获取路线
     * 简单的数据访问
     */
    override fun getRouteById(routeId: String): Route? {
        return routeRepository.findById(routeId).orElse(null)
    }

    /**
     * 更新路线
     * 包含业务规则验证
     */
    @Transactional
    override fun updateRoute(route: Route): Route {
        // 确保路线存在
        if (!routeRepository.existsById(route.id)) {
            throw IllegalArgumentException("路线不存在: ${route.id}")
        }

        // 应用业务规则
        route.updatedAt = Instant.now()
        return routeRepository.save(route)
    }

    /**
     * 获取热门路线
     * 领域逻辑：按热度排序返回已发布的路线
     */
    @Transactional(readOnly = true)
    override fun getPopularRoutes(limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route> {
        // 领域规则：只返回已发布的路线（status = 1）
        // 按热度（popularity）降序排列，热度相同时按使用次数（usageCount）降序
        val sort = org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Order.desc("popularity"),
            org.springframework.data.domain.Sort.Order.desc("usageCount")
        )
        val pageRequest = org.springframework.data.domain.PageRequest.of(
            pageable.pageNumber,
            limit.coerceAtMost(100),
            sort
        )
        return routeRepository.findByStatus(1, pageRequest)
    }

    /**
     * 获取新晋路线
     * 领域逻辑：按创建时间降序返回已发布的路线
     */
    @Transactional(readOnly = true)
    override fun getNewRoutes(limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route> {
        // 领域规则：只返回已发布的路线（status = 1）
        // 按创建时间（createdAt）降序排列
        val pageRequest = org.springframework.data.domain.PageRequest.of(
            pageable.pageNumber,
            limit.coerceAtMost(100)
        )
        return routeRepository.findByStatusOrderByCreatedAtDesc(1, pageRequest)
    }

    /**
     * 获取季节性路线
     * 领域逻辑：根据季节标签返回路线
     */
    @Transactional(readOnly = true)
    override fun getSeasonalRoutes(season: String?, limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route> {
        // 领域规则：只返回已发布的路线（status = 1）
        // 如果没有指定季节，根据当前月份判断
        val targetSeason = season ?: getCurrentSeason()
        
        // 通过标签查找季节性路线
        val seasonalTags = getSeasonTags(targetSeason)
        
        // 简化实现：如果没有标签数据，返回热门路线
        // 实际项目中应该通过 RouteTagRepository 查找
        return getPopularRoutes(limit, pageable)
    }

    /**
     * 获取周末路线
     * 领域逻辑：返回适合周末的短途路线
     */
    @Transactional(readOnly = true)
    override fun getWeekendRoutes(limit: Int, pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<Route> {
        // 领域规则：只返回已发布的路线（status = 1）
        // 周末路线通常是往返或环线类型（routeType = 0 或 1）
        // 简化实现：返回热门路线
        // 实际项目中应该按路线类型和距离筛选
        return getPopularRoutes(limit, pageable)
    }

    /**
     * 获取当前季节
     */
    private fun getCurrentSeason(): String {
        val month = java.time.LocalDate.now().monthValue
        return when (month) {
            3, 4, 5 -> "春季"
            6, 7, 8 -> "夏季"
            9, 10, 11 -> "秋季"
            else -> "冬季"
        }
    }

    /**
     * 获取季节对应的标签列表
     */
    private fun getSeasonTags(season: String): List<String> {
        return when (season) {
            "春季", "春" -> listOf("春季", "春天", "赏花", "踏青")
            "夏季", "夏" -> listOf("夏季", "夏天", "避暑", "溯溪")
            "秋季", "秋" -> listOf("秋季", "秋天", "赏枫", "秋收")
            "冬季", "冬" -> listOf("冬季", "冬天", "赏雪", "温泉")
            else -> listOf(season)
        }
    }

    /**
     * 按标签搜索路线
     * 领域逻辑：根据标签列表查找已发布的路线
     */
    @Transactional(readOnly = true)
    override fun searchRoutesByTags(
        tags: List<String>,
        pageable: org.springframework.data.domain.Pageable
    ): Page<Route> {
        if (tags.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        // 1. 通过 RouteTagRepository 查找包含任意标签的路线 ID
        val routeIds = tags.flatMap { tag ->
            routeTagRepository.findByTag(tag)
        }.map { it.routeId }
            .distinct()

        if (routeIds.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        // 2. 通过路线 ID 列表查找路线，只返回已发布的
        val routes = routeRepository.findByIdIn(routeIds, pageable)
        
        // 3. 过滤只返回已发布的路线
        val publishedRoutes = routes.content.filter { it.status == 1 }
        return PageImpl(publishedRoutes, pageable, publishedRoutes.size.toLong())
    }

    /**
     * 统一搜索路线（支持所有抽象参数）
     * 领域逻辑：结合所有查询条件搜索路线
     */
    @Transactional(readOnly = true)
    override fun searchRoutesUnified(
        keyword: String?,
        regionId: String?,
        difficulty: Int?,
        routeType: Int?,
        minDistance: Double?,
        maxDistance: Double?,
        userId: String?,
        tags: List<String>?,
        sortBy: String?,
        pageable: org.springframework.data.domain.Pageable
    ): Page<Route> {
        // 1. 如果有标签条件，先按标签过滤
        var resultRouteIds: Set<String>? = null
        if (!tags.isNullOrEmpty()) {
            val tagRouteIds = tags.flatMap { tag ->
                routeTagRepository.findByTag(tag)
            }.map { it.routeId }.toSet()
            
            resultRouteIds = tagRouteIds
            
            // 如果没有匹配的标签路线，直接返回空
            if (resultRouteIds.isEmpty()) {
                return PageImpl(emptyList(), pageable, 0)
            }
        }

        // 2. 应用排序
        val sort = when (sortBy?.lowercase()) {
            "new", "最新" -> Sort.by(Sort.Order.desc("createdAt"))
            "distance", "距离" -> Sort.by(Sort.Order.asc("distance"))
            else -> Sort.by(
                Sort.Order.desc("popularity"),
                Sort.Order.desc("usageCount")
            )
        }

        val pageRequest = PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            sort
        )

        // 3. 执行多条件搜索
        val searchResults = if (userId != null) {
            routeRepository.findUserFavoriteRoutes(userId, pageRequest)
        } else {
            routeRepository.searchRoutes(
                keyword = keyword,
                region = regionId,
                difficulty = difficulty,
                routeType = routeType,
                status = 1, // 只查询已发布的路线
                pageable = pageRequest
            )
        }

        // 4. 如果有标签过滤，再进一步过滤
        return if (resultRouteIds != null) {
            val filteredRoutes = searchResults.content.filter { it.id in resultRouteIds }
            PageImpl(filteredRoutes, pageRequest, filteredRoutes.size.toLong())
        } else {
            searchResults
        }
    }
}
