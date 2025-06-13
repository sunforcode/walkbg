package org.example.route.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.route.model.Route

/**
 * 路线领域服务接口
 */
interface RouteService {
    /**
     * 根据ID查询路线
     */
    fun findById(id: String): Route?

    /**
     * 分页查询路线
     */
    fun findAll(pageable: Pageable): Page<Route>

    /**
     * 根据条件分页查询路线
     */
    fun findByCondition(
        keyword: String? = null,
        regionId: String? = null,
        difficulty: Int? = null,
        routeType: Int? = null,
        minDistance: Double? = null,
        maxDistance: Double? = null,
        tags: List<String>? = null,
        userId: String? = null,
        pageable: Pageable
    ): Page<Route>

    /**
     * 创建路线
     */
    fun create(route: Route): Route

    /**
     * 更新路线
     */
    fun update(route: Route): Route

    /**
     * 删除路线
     */
    fun delete(id: String): Boolean

    /**
     * 增加路线热度
     */
    fun incrementPopularity(id: String): Boolean

    /**
     * 增加路线使用次数
     */
    fun incrementUsageCount(id: String): Boolean

    /**
     * 收藏路线
     */
    fun favorite(routeId: String, userId: String): Boolean

    /**
     * 取消收藏路线
     */
    fun unfavorite(routeId: String, userId: String): Boolean

    /**
     * 检查路线是否被收藏
     */
    fun isFavorite(routeId: String, userId: String): Boolean
}
