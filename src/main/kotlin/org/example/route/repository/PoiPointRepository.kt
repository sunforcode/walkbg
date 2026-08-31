package org.example.route.repository

import org.example.route.model.PoiPoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PoiPointRepository : JpaRepository<PoiPoint, String> {

    /** 查询路线的所有 POI 点 */
    fun findByRouteId(@Param("routeId") routeId: String): List<PoiPoint>

    /** 按数据状态查询路线的 POI 点（draft/confirmed） */
    fun findByRouteIdAndStatus(
        @Param("routeId") routeId: String,
        @Param("status") status: String
    ): List<PoiPoint>

    /** 按 category 查询路线的 POI 点 */
    fun findByRouteIdAndCategory(
        @Param("routeId") routeId: String,
        @Param("category") category: String
    ): List<PoiPoint>

    /** 按 source 查询路线的 POI 点 */
    fun findByRouteIdAndSource(
        @Param("routeId") routeId: String,
        @Param("source") source: String
    ): List<PoiPoint>

    /** 删除路线的所有 POI 点 */
    fun deleteByRouteId(@Param("routeId") routeId: String): Long
}
