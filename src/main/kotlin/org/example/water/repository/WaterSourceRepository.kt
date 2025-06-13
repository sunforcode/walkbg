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
    fun findByRouteId(routeId: String, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据水源类型查找
     */
    fun findByWaterTypeAndIsActiveTrue(waterType: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据水质查找
     */
    fun findByWaterQualityAndIsActiveTrue(waterQuality: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据路线ID和水源类型查找
     */
    fun findByRouteIdAndWaterTypeAndIsActiveTrue(routeId: String, waterType: Int, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据海拔范围查找水源
     */
    fun findByElevationBetweenAndIsActiveTrue(minElevation: BigDecimal, maxElevation: BigDecimal, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据名称模糊查询水源
     */
    fun findByNameContainingIgnoreCaseAndIsActiveTrue(name: String, pageable: Pageable): Page<WaterSource>
    
    /**
     * 根据路线ID查找水源，按海拔排序
     */
    fun findByRouteIdAndIsActiveTrueOrderByElevationAsc(routeId: String): List<WaterSource>
    
    /**
     * 查找不需要处理的水源
     */
    fun findByRequiresTreatmentFalseAndIsActiveTrue(pageable: Pageable): Page<WaterSource>
    
    /**
     * 复合查询：根据多个条件查找水源
     */
    @Query("SELECT ws FROM WaterSource ws WHERE " +
           "ws.isActive = true AND " +
           "(:routeId IS NULL OR ws.routeId = :routeId) AND " +
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
    fun countByRouteIdAndIsActiveTrue(routeId: String): Long
}