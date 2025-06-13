package org.example.route.service

import org.example.route.model.Campsite
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

interface CampsiteService {
    
    /**
     * 获取所有营地（分页）
     */
    fun getAllCampsites(pageable: Pageable): Page<Campsite>
    
    /**
     * 根据ID获取营地
     */
    fun getCampsiteById(id: String): Campsite?
    
    /**
     * 创建新营地
     */
    fun createCampsite(campsite: Campsite): Campsite
    
    /**
     * 更新营地信息
     */
    fun updateCampsite(id: String, campsite: Campsite): Campsite?
    
    /**
     * 删除营地（软删除）
     */
    fun deleteCampsite(id: String): Boolean
    
    /**
     * 根据路线ID获取营地
     */
    fun getCampsitesByRoute(routeId: String, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据路线ID获取营地（按海拔排序）
     */
    fun getCampsitesByRouteSorted(routeId: String): List<Campsite>
    
    /**
     * 根据营地类型获取营地
     */
    fun getCampsitesByType(campsiteType: Int, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据路线ID和营地类型获取营地
     */
    fun getCampsitesByRouteAndType(routeId: String, campsiteType: Int, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据海拔范围获取营地
     */
    fun getCampsitesByElevationRange(minElevation: BigDecimal?, maxElevation: BigDecimal?, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据名称搜索营地
     */
    fun searchCampsitesByName(name: String, pageable: Pageable): Page<Campsite>
    
    /**
     * 复合条件搜索营地
     */
    fun searchCampsitesWithFilters(
        routeId: String?,
        campsiteType: Int?,
        minElevation: BigDecimal?,
        maxElevation: BigDecimal?,
        name: String?,
        pageable: Pageable
    ): Page<Campsite>
    
    /**
     * 统计路线的营地数量
     */
    fun countCampsitesByRoute(routeId: String): Long
    
    /**
     * 验证营地信息
     */
    fun verifyCampsite(id: String, verifiedBy: String): Campsite?
}