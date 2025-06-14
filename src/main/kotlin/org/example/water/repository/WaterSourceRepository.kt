package org.example.water.repository

import org.example.water.model.WaterSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface WaterSourceRepository : JpaRepository<WaterSource, String> {
    
    /**
     * 根据路线ID查找水源
     */
    @Query("SELECT ws FROM WaterSource ws JOIN ws.route r WHERE r.id = :routeId")
    fun findByRouteId(@Param("routeId") routeId: String, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据水源类型查找
     */
    fun findByWaterType(waterType: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据水质查找
     */
    fun findByWaterQuality(waterQuality: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据路线ID和水源类型查找
     */
    @Query("SELECT ws FROM WaterSource ws JOIN ws.route r WHERE r.id = :routeId AND ws.waterType = :waterType")
    fun findByRouteIdAndWaterType(@Param("routeId") routeId: String, @Param("waterType") waterType: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据海拔范围查找水源
     */
    fun findByElevationBetween(minElevation: BigDecimal, maxElevation: BigDecimal, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据名称模糊查询水源
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据路线ID查找水源，按海拔排序
     */
    @Query("SELECT ws FROM WaterSource ws JOIN ws.route r WHERE r.id = :routeId ORDER BY ws.elevation ASC")
    fun findByRouteIdOrderByElevationAsc(@Param("routeId") routeId: String): List<WaterSource>
    
    /**
     * 查找不需要处理的水源
     */
    fun findByRequiresTreatmentFalse(pageable: Pageable): Page<WaterSource>
    
    /**
     * 复合查询：根据多个条件查找水源
     */
    @Query("SELECT ws FROM WaterSource ws LEFT JOIN ws.route r WHERE " +
           "(:routeId IS NULL OR r.id = :routeId) AND " +
           "(:waterType IS NULL OR ws.waterType = :waterType) AND " +
           "(:waterQuality IS NULL OR ws.waterQuality = :waterQuality) AND " +
           "(:minElevation IS NULL OR ws.elevation >= :minElevation) AND " +
           "(:maxElevation IS NULL OR ws.elevation <= :maxElevation) AND " +
           "(:requiresTreatment IS NULL OR ws.requiresTreatment = :requiresTreatment) AND " +
           "(:name IS NULL OR LOWER(ws.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    fun findWaterSourcesWithFilters(
        @Param("routeId") routeId: String?,
        @Param("waterType") waterType: Int?,
        @Param("waterQuality") waterQuality: Int?,
        @Param("minElevation") minElevation: BigDecimal?,
        @Param("maxElevation") maxElevation: BigDecimal?,
        @Param("requiresTreatment") requiresTreatment: Boolean?,
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<WaterSource>
    
    /**
     * 统计路线的水源数量
     */
    @Query("SELECT COUNT(ws) FROM WaterSource ws JOIN ws.route r WHERE r.id = :routeId")
    fun countByRouteId(@Param("routeId") routeId: String): Long
}