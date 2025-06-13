package org.example.route.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.route.model.Campsite
import java.math.BigDecimal

@Repository
interface CampsiteRepository : JpaRepository<Campsite, String> {
    
    /**
     * 根据路线ID查找营地
     */
    @Query("SELECT c FROM Campsite c JOIN c.route r WHERE r.id = :routeId AND c.isActive = true")
    fun findByRouteIdAndIsActiveTrue(@Param("routeId") routeId: String, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据营地类型查找
     */
    fun findByCampsiteTypeAndIsActiveTrue(campsiteType: Int, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据路线ID和营地类型查找
     */
    @Query("SELECT c FROM Campsite c JOIN c.route r WHERE r.id = :routeId AND c.campsiteType = :campsiteType AND c.isActive = true")
    fun findByRouteIdAndCampsiteTypeAndIsActiveTrue(@Param("routeId") routeId: String, @Param("campsiteType") campsiteType: Int, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据海拔范围查找营地
     */
    fun findByElevationBetweenAndIsActiveTrue(minElevation: BigDecimal, maxElevation: BigDecimal, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据名称模糊查询营地
     */
    fun findByNameContainingIgnoreCaseAndIsActiveTrue(name: String, pageable: Pageable): Page<Campsite>
    
    /**
     * 根据路线ID查找营地，按海拔排序
     */
    @Query("SELECT c FROM Campsite c JOIN c.route r WHERE r.id = :routeId AND c.isActive = true ORDER BY c.elevation ASC")
    fun findByRouteIdAndIsActiveTrueOrderByElevationAsc(@Param("routeId") routeId: String): List<Campsite>
    
    /**
     * 复合查询：根据多个条件查找营地
     */
    @Query("SELECT c FROM Campsite c LEFT JOIN c.route r WHERE " +
           "c.isActive = true AND " +
           "(:routeId IS NULL OR r.id = :routeId) AND " +
           "(:campsiteType IS NULL OR c.campsiteType = :campsiteType) AND " +
           "(:minElevation IS NULL OR c.elevation >= :minElevation) AND " +
           "(:maxElevation IS NULL OR c.elevation <= :maxElevation) AND " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    fun findCampsitesWithFilters(
        @Param("routeId") routeId: String?,
        @Param("campsiteType") campsiteType: Int?,
        @Param("minElevation") minElevation: BigDecimal?,
        @Param("maxElevation") maxElevation: BigDecimal?,
        @Param("name") name: String?,
        pageable: Pageable
    ): Page<Campsite>
    
    /**
     * 统计路线的营地数量
     */
    @Query("SELECT COUNT(c) FROM Campsite c JOIN c.route r WHERE r.id = :routeId AND c.isActive = true")
    fun countByRouteIdAndIsActiveTrue(@Param("routeId") routeId: String): Long
}