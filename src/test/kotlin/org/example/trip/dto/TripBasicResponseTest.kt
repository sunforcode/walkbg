package org.example.trip.dto

import org.example.trip.model.Trip
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 覆盖 route-to-trip-linkage change 中响应侧路线集合的来源规则。
 *
 * 对应 spec: trip-route-linkage
 * - Requirement: 行程响应中的路线集合来自真实关联事实
 */
class TripBasicResponseTest {

    private fun trip(primaryRouteId: String? = null) = Trip(
        id = "trip-1",
        name = "夏季徒步之旅",
        organizerId = "user-1",
        primaryRouteId = primaryRouteId,
        status = 0
    )

    // ========== 关联记录存在时以关联记录为准 ==========

    @Test
    fun `多路线行程完整返回关联的全部路线`() {
        val response = TripBasicResponse.fromTrip(
            trip(primaryRouteId = "route-a"),
            routeIds = listOf("route-a", "route-b", "route-c")
        )

        assertEquals(listOf("route-a", "route-b", "route-c"), response.routeIds)
    }

    @Test
    fun `单路线行程返回形态保持不变`() {
        val response = TripBasicResponse.fromTrip(
            trip(primaryRouteId = "route-a"),
            routeIds = listOf("route-a")
        )

        assertEquals(listOf("route-a"), response.routeIds)
    }

    @Test
    fun `关联记录存在时结果不受primaryRouteId影响`() {
        // 关联记录含两条路线，primaryRouteId 仅为其中之一；
        // 若仍走「由单值推导数组」的旧逻辑，结果会退化为单元素集合。
        val response = TripBasicResponse.fromTrip(
            trip(primaryRouteId = "route-a"),
            routeIds = listOf("route-a", "route-b")
        )

        assertEquals(listOf("route-a", "route-b"), response.routeIds)
    }

    // ========== 历史行程：无关联记录时回退推导 ==========

    @Test
    fun `无关联记录的历史行程回退推导primaryRouteId`() {
        val response = TripBasicResponse.fromTrip(trip(primaryRouteId = "route-legacy"), routeIds = null)

        assertEquals(listOf("route-legacy"), response.routeIds)
    }

    @Test
    fun `传入空关联列表的历史行程同样回退推导`() {
        val response = TripBasicResponse.fromTrip(trip(primaryRouteId = "route-legacy"), routeIds = emptyList())

        assertEquals(listOf("route-legacy"), response.routeIds)
    }

    @Test
    fun `无关联记录且无主路线时返回空集合`() {
        val response = TripBasicResponse.fromTrip(trip(primaryRouteId = null), routeIds = null)

        assertEquals(emptyList<String>(), response.routeIds)
    }
}
