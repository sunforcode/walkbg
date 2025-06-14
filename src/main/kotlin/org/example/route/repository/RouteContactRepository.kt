package org.example.route.repository

import org.example.route.model.RouteContact
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RouteContactRepository : JpaRepository<RouteContact, String> {
    
    /**
     * 根据路线ID查找联系人关联
     */
    fun findByRouteId(routeId: String, pageable: Pageable): Page<RouteContact>
    
    /**
     * 根据联系人ID查找路线关联
     */
    fun findByContactId(contactId: String, pageable: Pageable): Page<RouteContact>
    
    /**
     * 根据路线ID和联系人类型查找
     */
    fun findByRouteIdAndContactType(routeId: String, contactType: Int, pageable: Pageable): Page<RouteContact>
    
    /**
     * 根据路线ID查找，按优先级排序
     */
    fun findByRouteIdOrderByPriorityAsc(routeId: String): List<RouteContact>
    
    /**
     * 检查路线和联系人是否已关联
     */
    fun existsByRouteIdAndContactId(routeId: String, contactId: String): Boolean
    
    /**
     * 根据路线ID和联系人类型查找，按优先级排序
     */
    fun findByRouteIdAndContactTypeOrderByPriorityAsc(routeId: String, contactType: Int): List<RouteContact>
    
    /**
     * 复合查询：根据多个条件查找路线联系人关联
     */
    @Query("SELECT rc FROM RouteContact rc WHERE " +
           "rc.routeId = :routeId AND " +
           "(:contactType IS NULL OR rc.contactType = :contactType) " +
           "ORDER BY rc.priority ASC")
    fun findRouteContactsWithFilters(
        @Param("routeId") routeId: String,
        @Param("contactType") contactType: Int?
    ): List<RouteContact>
}