package org.example.meal.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.route.model.Supply
import java.math.BigDecimal

interface SupplyService {
    
    /**
     * 获取所有补给点（分页）
     */
    fun getAllSupplies(pageable: Pageable): Page<Supply>
    
    /**
     * 根据ID获取补给点
     */
    fun getSupplyById(id: String): Supply?
    
    /**
     * 创建新补给点
     */
    fun createSupply(supply: Supply): Supply
    
    /**
     * 更新补给点信息
     */
    fun updateSupply(id: String, supply: Supply): Supply?
    
    /**
     * 删除补给点（软删除）
     */
    fun deleteSupply(id: String): Boolean
    
    /**
     * 根据路线ID获取补给点
     */
    fun getSuppliesByRoute(routeId: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据路线ID获取补给点（按海拔排序）
     */
    fun getSuppliesByRouteSorted(routeId: String): List<Supply>
    
    /**
     * 根据补给类型获取补给点
     */
    fun getSuppliesByType(supplyType: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据路线ID和补给类型获取补给点
     */
    fun getSuppliesByRouteAndType(routeId: String, supplyType: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据海拔范围获取补给点
     */
    fun getSuppliesByElevationRange(minElevation: BigDecimal?, maxElevation: BigDecimal?, pageable: Pageable): Page<Supply>
    
    /**
     * 根据名称搜索补给点
     */
    fun searchSuppliesByName(name: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据价格范围获取补给点
     */
    fun getSuppliesByPriceRange(priceRange: String, pageable: Pageable): Page<Supply>
    
    /**
     * 复合条件搜索补给点
     */
    fun searchSuppliesWithFilters(
        routeId: String?,
        supplyType: String?,
        priceRange: String?,
        minElevation: BigDecimal?,
        maxElevation: BigDecimal?,
        name: String?,
        pageable: Pageable
    ): Page<Supply>
    
    /**
     * 统计路线的补给点数量
     */
    fun countSuppliesByRoute(routeId: String): Long
    
    /**
     * 更新补给点验证信息
     */
    fun updateSupplyVerification(id: String, verifiedBy: String): Supply?
}