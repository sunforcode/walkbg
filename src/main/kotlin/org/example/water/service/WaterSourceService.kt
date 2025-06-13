package org.example.water.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.water.model.WaterSource
import java.math.BigDecimal

interface WaterSourceService {
    
    /**
     * 获取所有水源（分页）
     */
    fun getAllWaterSources(pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据ID获取水源
     */
    fun getWaterSourceById(id: String): WaterSource?
    
    /**
     * 创建新水源
     */
    fun createWaterSource(waterSource: WaterSource): WaterSource
    
    /**
     * 更新水源信息
     */
    fun updateWaterSource(id: String, waterSource: WaterSource): WaterSource?
    
    /**
     * 删除水源（软删除）
     */
    fun deleteWaterSource(id: String): Boolean
    
    /**
     * 根据路线ID获取水源
     */
    fun getWaterSourcesByRoute(routeId: String, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据路线ID获取水源（按海拔排序）
     */
    fun getWaterSourcesByRouteSorted(routeId: String): List<WaterSource>
    
    /**
     * 根据水源类型获取水源
     */
    fun getWaterSourcesByType(waterType: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据水质获取水源
     */
    fun getWaterSourcesByQuality(waterQuality: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据路线ID和水源类型获取水源
     */
    fun getWaterSourcesByRouteAndType(routeId: String, waterType: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据海拔范围获取水源
     */
    fun getWaterSourcesByElevationRange(minElevation: BigDecimal?, maxElevation: BigDecimal?, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据名称搜索水源
     */
    fun searchWaterSourcesByName(name: String, pageable: Pageable): Page<WaterSource>
    
    /**
     * 获取不需要处理的水源
     */
    fun getSafeWaterSources(pageable: Pageable): Page<WaterSource>
    
    /**
     * 复合条件搜索水源
     */
    fun searchWaterSourcesWithFilters(
        routeId: String?,
        waterType: Int?,
        waterQuality: Int?,
        minElevation: BigDecimal?,
        maxElevation: BigDecimal?,
        requiresTreatment: Boolean?,
        name: String?,
        pageable: Pageable
    ): Page<WaterSource>
    
    /**
     * 统计路线的水源数量
     */
    fun countWaterSourcesByRoute(routeId: String): Long
    
    /**
     * 验证水源信息
     */
    fun verifyWaterSource(id: String, verifiedBy: String): WaterSource?
}