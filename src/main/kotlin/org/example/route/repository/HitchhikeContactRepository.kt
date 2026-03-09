package org.example.route.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.example.route.model.HitchhikeContact

@Repository
interface HitchhikeContactRepository : JpaRepository<HitchhikeContact, String> {
    
    /**
     * 根据路线ID查找所有搭车联系人
     */
    fun findByRouteId(routeId: String): List<HitchhikeContact>
    
    /**
     * 统计路线的搭车联系人数量
     */
    fun countByRouteId(routeId: String): Long
    
    /**
     * 删除路线的所有搭车联系人
     */
    fun deleteByRouteId(routeId: String): Long
}
