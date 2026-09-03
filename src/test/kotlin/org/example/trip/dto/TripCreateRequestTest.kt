package org.example.trip.dto

import org.example.common.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * 覆盖 route-to-trip-linkage change 中路线集合与主路线的归一化与校验规则。
 *
 * 对应 spec: trip-route-linkage
 * - Requirement: 主路线标识与路线集合的语义约束
 * - Requirement: 创建行程必须至少包含一条路线
 */
class TripCreateRequestTest {

    private fun request(
        routeIds: List<String>? = null,
        primaryRouteId: String? = null
    ) = TripCreateRequest(
        name = "夏季徒步之旅",
        routeIds = routeIds,
        primaryRouteId = primaryRouteId
    )

    // ========== 归一化：route_ids 缺省时回退为 primary_route_id 单元素集合 ==========

    @Test
    fun `仅提供primary_route_id时视为单元素集合`() {
        val resolved = request(primaryRouteId = "route-1").resolveRoutes()

        assertEquals(listOf("route-1"), resolved.routeIds)
        assertEquals("route-1", resolved.primaryRouteId)
    }

    @Test
    fun `提供单条route_ids时主路线为该路线`() {
        val resolved = request(routeIds = listOf("route-1")).resolveRoutes()

        assertEquals(listOf("route-1"), resolved.routeIds)
        assertEquals("route-1", resolved.primaryRouteId)
    }

    // ========== 多路线：缺省主路线时自动取首个元素 ==========

    @Test
    fun `多路线缺省主路线时自动取首个元素`() {
        val resolved = request(routeIds = listOf("route-a", "route-b", "route-c")).resolveRoutes()

        assertEquals(listOf("route-a", "route-b", "route-c"), resolved.routeIds)
        assertEquals("route-a", resolved.primaryRouteId)
    }

    @Test
    fun `多路线显式指定主路线时保留该指定`() {
        val resolved = request(
            routeIds = listOf("route-a", "route-b"),
            primaryRouteId = "route-b"
        ).resolveRoutes()

        assertEquals(listOf("route-a", "route-b"), resolved.routeIds)
        assertEquals("route-b", resolved.primaryRouteId)
    }

    @Test
    fun `重复路线标识被去重且保持给定顺序`() {
        val resolved = request(routeIds = listOf("route-b", "route-a", "route-b")).resolveRoutes()

        assertEquals(listOf("route-b", "route-a"), resolved.routeIds)
        assertEquals("route-b", resolved.primaryRouteId)
    }

    // ========== 校验：主路线必须属于路线集合 ==========

    @Test
    fun `主路线不在route_ids中时抛出校验异常`() {
        val exception = assertThrows(BusinessException::class.java) {
            request(routeIds = listOf("route-a", "route-b"), primaryRouteId = "route-x").resolveRoutes()
        }

        assertEquals("VALIDATION_ERROR", exception.errorCode)
        assertEquals("primary_route_id", exception.details?.get("field"))
    }

    // ========== 校验：至少包含一条路线 ==========

    @Test
    fun `未提供任何路线标识时抛出校验异常`() {
        val exception = assertThrows(BusinessException::class.java) {
            request().resolveRoutes()
        }

        assertEquals("VALIDATION_ERROR", exception.errorCode)
        assertEquals("route_ids", exception.details?.get("field"))
    }

    @Test
    fun `显式传入空route_ids时抛出校验异常`() {
        val exception = assertThrows(BusinessException::class.java) {
            request(routeIds = emptyList()).resolveRoutes()
        }

        assertEquals("VALIDATION_ERROR", exception.errorCode)
        assertEquals("route_ids", exception.details?.get("field"))
    }

    @Test
    fun `route_ids仅含空白字符串时视为空集合并被拒绝`() {
        val exception = assertThrows(BusinessException::class.java) {
            request(routeIds = listOf("", "   ")).resolveRoutes()
        }

        assertEquals("VALIDATION_ERROR", exception.errorCode)
        assertEquals("route_ids", exception.details?.get("field"))
    }
}
