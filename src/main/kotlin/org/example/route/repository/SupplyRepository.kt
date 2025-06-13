package org.example.route.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.route.model.Supply
import java.math.BigDecimal

@Repository
interface SupplyRepository : JpaRepository<Supply, String> {
    
    /**
     * 根据路线ID查找补给点
     */
    fun findByRouteIdAndIsActiveTrue(routeId: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据补给类型查找
     */
    fun findBySupplyTypeAndIsActiveTrue(supplyType: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据路线ID和补给类型查找
     */
    fun findByRouteIdAndSupplyTypeAndIsActiveTrue(routeId: String, supplyType: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据海拔范围查找补给点
     */
    fun findByElevationBetweenAndIsActiveTrue(minElevation: BigDecimal, maxElevation: BigDecimal, pageable: Pageable): Page<Supply>
    
    /**
     * 根据名称模糊查询补给点
     */
    fun findByNameContainingIgnoreCaseAndIsActiveTrue(name: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据路线ID查找补给点，按海拔排序
     */
    fun findByRouteIdAndIsActiveTrueOrderByElevationAsc(routeId: String): List<Supply>
    
    /**
     * 根据价格范围查找补给点
     */
    fun findByPriceRangeAndIsActiveTrue(priceRange: String, pageable: Pageable): Page<Supply>
    
    /**
     * 复合查询：根据多个条件查找补给点
     */
    @Query("SELECT s FROM Supply s WHERE " +
           "s.isActive = true AND " +
           "(:routeId IS NULL OR s.routeId = :routeId) AND " +
           "(:supplyType IS NULL OR s.supplyType = :supplyType) AND " +
           "(:priceRange IS NULL OR s.priceRange = :priceRange) AND " +
           "(:minElevation IS NULL OR s.elevation >= :minElevation) AND " +
           "(:maxElevation IS NULL OR s.elevation <= :maxElevation) AND " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    fun findSuppliesWithFilters(
        @Param("routeId") routeId: String?,
        @Param("supplyType") supplyType: String?,
        @Param("priceRange") priceRange: String?,
        @Param("minElevation") minElevation: BigDecimal?,
        @Param("maxElevation") maxElevation: BigDecimal?,
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<Supply>
    
    /**
     * 统计路线的补给点数量
     */
    fun countByRouteIdAndIsActiveTrue(routeId: String): Long
}