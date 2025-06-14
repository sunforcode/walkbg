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
    @Query("SELECT s FROM Supply s JOIN s.route r WHERE r.id = :routeId")
    fun findByRouteId(@Param("routeId") routeId: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据补给类型查找
     */
    fun findBySupplyType(supplyType: Int, pageable: Pageable): Page<Supply>
    
    /**
     * 根据路线ID和补给类型查找
     */
    @Query("SELECT s FROM Supply s JOIN s.route r WHERE r.id = :routeId AND s.supplyType = :supplyType")
    fun findByRouteIdAndSupplyType(@Param("routeId") routeId: String, @Param("supplyType") supplyType: Int, pageable: Pageable): Page<Supply>
    
    /**
     * 根据海拔范围查找补给点
     */
    fun findByElevationBetween(minElevation: BigDecimal, maxElevation: BigDecimal, pageable: Pageable): Page<Supply>
    
    /**
     * 根据名称模糊查询补给点
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Supply>
    
    /**
     * 根据路线ID查找补给点，按海拔排序
     */
    @Query("SELECT s FROM Supply s JOIN s.route r WHERE r.id = :routeId ORDER BY s.elevation ASC")
    fun findByRouteIdOrderByElevationAsc(@Param("routeId") routeId: String): List<Supply>
    
    // TODO: 价格范围功能暂时移除，因为 Supply 实体中没有 priceRange 属性
    // fun findByPriceRange(priceRange: String, pageable: Pageable): Page<Supply>
    
    /**
     * 复合查询：根据多个条件查找补给点
     */
    @Query("SELECT s FROM Supply s LEFT JOIN s.route r WHERE " +
           "(:routeId IS NULL OR r.id = :routeId) AND " +
           "(:supplyType IS NULL OR s.supplyType = :supplyType) AND " +
           "(:minElevation IS NULL OR s.elevation >= :minElevation) AND " +
           "(:maxElevation IS NULL OR s.elevation <= :maxElevation) AND " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    fun findSuppliesWithFilters(
        @Param("routeId") routeId: String?,
        @Param("supplyType") supplyType: Int?,
        @Param("minElevation") minElevation: BigDecimal?,
        @Param("maxElevation") maxElevation: BigDecimal?,
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<Supply>
    
    /**
     * 统计路线的补给点数量
     */
    @Query("SELECT COUNT(s) FROM Supply s JOIN s.route r WHERE r.id = :routeId")
    fun countByRouteId(@Param("routeId") routeId: String): Long
}