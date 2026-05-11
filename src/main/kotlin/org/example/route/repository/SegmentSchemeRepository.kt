package org.example.route.repository

import org.example.route.model.SegmentScheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SegmentSchemeRepository : JpaRepository<SegmentScheme, String> {

    /** 查询路线的所有分段方案（元数据，不含 segments 详情） */
    fun findByRouteId(@Param("routeId") routeId: String): List<SegmentScheme>

    /** 查询路线的默认分段方案 */
    fun findByRouteIdAndIsDefaultTrue(@Param("routeId") routeId: String): SegmentScheme?

    /** 查询路线指定类型的分段方案 */
    fun findByRouteIdAndSchemeType(
        @Param("routeId") routeId: String,
        @Param("schemeType") schemeType: String
    ): SegmentScheme?

    /** 删除路线的所有分段方案 */
    fun deleteByRouteId(@Param("routeId") routeId: String): Long
}
