package org.example.route.service

import org.example.infrastructure.repository.RouteRepository
import org.example.user.repository.UserRepository
import org.example.user.repository.UserFavoriteRouteRepository
import org.example.user.model.UserFavoriteRoute
import org.example.route.model.Route

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import jakarta.persistence.criteria.Predicate

/**
 * 路线领域服务实现
 */
@Service
class RouteServiceImpl(
    private val routeRepository: RouteRepository,
    private val userRepository: UserRepository,
    private val userFavoriteRouteRepository: UserFavoriteRouteRepository
) : RouteService {
    /**
     * 根据ID查询路线
     */
    override fun findById(id: String): Route? {
        return routeRepository.findById(id).orElse(null)
    }

    /**
     * 分页查询路线
     */
    override fun findAll(pageable: Pageable): Page<Route> {
        return routeRepository.findAll(pageable)
    }

    /**
     * 根据条件分页查询路线
     */
    override fun findByCondition(
        keyword: String?,
        regionId: String?,
        difficulty: Int?,
        routeType: Int?,
        minDistance: Double?,
        maxDistance: Double?,
        tags: List<String>?,
        userId: String?,
        pageable: Pageable
    ): Page<Route> {
        // 使用Specification构建动态查询条件
        val spec = Specification<Route> { root, query, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            // 关键字查询
            keyword?.let {
                val keywordLike = "%$it%"
                val nameLike = criteriaBuilder.like(root.get("name"), keywordLike)
                val descriptionLike = criteriaBuilder.like(root.get("description"), keywordLike)
                predicates.add(criteriaBuilder.or(nameLike, descriptionLike))
            }

            // 地区ID查询
            regionId?.let {
                predicates.add(criteriaBuilder.equal(root.get<String>("regionId"), it))
            }

            // 难度查询
            difficulty?.let {
                predicates.add(criteriaBuilder.equal(root.get<Int>("difficulty"), it))
            }

            // 路线类型查询
            routeType?.let {
                predicates.add(criteriaBuilder.equal(root.get<Int>("routeType"), it))
            }

            // 距离范围查询
            if (minDistance != null && maxDistance != null) {
                predicates.add(criteriaBuilder.between(root.get("distance"), minDistance, maxDistance))
            } else if (minDistance != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("distance"), minDistance))
            } else if (maxDistance != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("distance"), maxDistance))
            }

            // 标签查询 - 这里需要使用JOIN，比较复杂，暂时不实现

            // 用户收藏查询 - 这里需要使用JOIN，比较复杂，暂时不实现

            criteriaBuilder.and(*predicates.toTypedArray())
        }

        // 如果有标签查询，使用专门的方法
        if (!tags.isNullOrEmpty()) {
            return routeRepository.findByTags(tags, tags.size.toLong(), pageable)
        }

        // 如果是查询用户收藏，使用专门的方法
        if (userId != null) {
            return routeRepository.findFavoritesByUserId(userId, pageable)
        }

        return routeRepository.findAll(spec, pageable)
    }

    /**
     * 创建路线
     */
    @Transactional
    override fun create(route: Route): Route {
        return routeRepository.save(route)
    }

    /**
     * 更新路线
     */
    @Transactional
    override fun update(route: Route): Route {
        // 确保路线存在
        if (!routeRepository.existsById(route.id)) {
            throw IllegalArgumentException("路线不存在: ${route.id}")
        }


        return routeRepository.save(route)
    }

    /**
     * 删除路线
     */
    @Transactional
    override fun delete(id: String): Boolean {
        if (!routeRepository.existsById(id)) {
            return false
        }

        routeRepository.deleteById(id)
        return true
    }

    /**
     * 增加路线热度
     */
    @Transactional
    override fun incrementPopularity(id: String): Boolean {
        val result = routeRepository.incrementPopularity(id)
        return result > 0
    }

    /**
     * 增加路线使用次数
     */
    @Transactional
    override fun incrementUsageCount(id: String): Boolean {
        val result = routeRepository.incrementUsageCount(id)
        return result > 0
    }

    /**
     * 收藏路线
     */
    @Transactional
    override fun favorite(routeId: String, userId: String): Boolean {
        // 检查路线和用户是否存在
        val route = routeRepository.findById(routeId).orElse(null) ?: return false
        val user = userRepository.findById(userId).orElse(null) ?: return false

        // 检查是否已经收藏
        if (userFavoriteRouteRepository.existsByUserIdAndRouteId(userId, routeId)) {
            return true
        }

        // 创建收藏关系
        val favorite = UserFavoriteRoute(
            id = UUID.randomUUID().toString(),
            user = user,
            route = route
        )

        userFavoriteRouteRepository.save(favorite)
        return true
    }

    /**
     * 取消收藏路线
     */
    @Transactional
    override fun unfavorite(routeId: String, userId: String): Boolean {
        val result = userFavoriteRouteRepository.deleteByUserIdAndRouteId(userId, routeId)
        return result > 0
    }

    /**
     * 检查路线是否被收藏
     */
    override fun isFavorite(routeId: String, userId: String): Boolean {
        return userFavoriteRouteRepository.existsByUserIdAndRouteId(userId, routeId)
    }
}
