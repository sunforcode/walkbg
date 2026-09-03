package org.example.trip.controller

import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.service.EquipmentListItemService
import org.example.security.JwtAuthenticationFilter
import org.example.security.JwtTokenUtil
import org.example.trip.model.Trip
import org.example.trip.service.TripService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 覆盖 route-to-trip-linkage change 在 HTTP 契约层的行为：
 * 请求中的 `route_ids` 被真实消费，响应中的 `route_ids` 来自关联事实。
 *
 * 对应 spec: trip-route-linkage
 * - Requirement: 主路线标识与路线集合的语义约束
 * - Requirement: 创建行程必须至少包含一条路线
 * - Requirement: 行程响应中的路线集合来自真实关联事实
 */
@WebMvcTest(TripController::class)
@AutoConfigureMockMvc(addFilters = false)
class TripRouteLinkageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var tripService: TripService

    @MockBean
    private lateinit var equipmentListRepository: EquipmentListRepository

    @MockBean
    private lateinit var equipmentListItemService: EquipmentListItemService

    @MockBean
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private fun trip(primaryRouteId: String?) = Trip(
        id = "trip-1",
        name = "夏季徒步之旅",
        organizerId = "user-1",
        primaryRouteId = primaryRouteId,
        status = 0
    )

    // ========== 创建：route_ids 被真实消费 ==========

    @Test
    fun `POST - 创建多路线行程 - route_ids透传到服务层且响应返回全部路线`() {
        whenever(tripService.createTrip(any(), any(), any())).thenReturn(trip("route-a"))
        whenever(tripService.getRouteIds("trip-1")).thenReturn(listOf("route-a", "route-b"))

        mockMvc.perform(
            post("/api/v1/legacy/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "夏季徒步之旅",
                      "route_ids": ["route-a", "route-b"],
                      "primary_route_id": "route-a"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.route_ids.length()").value(2))
            .andExpect(jsonPath("$.data.route_ids[0]").value("route-a"))
            .andExpect(jsonPath("$.data.route_ids[1]").value("route-b"))

        val routeIds = argumentCaptor<List<String>>()
        val primary = argumentCaptor<String>()
        verify(tripService).createTrip(any(), routeIds.capture(), primary.capture())
        // 关键回归点：route_ids 不再被静默丢弃
        org.junit.jupiter.api.Assertions.assertEquals(listOf("route-a", "route-b"), routeIds.firstValue)
        org.junit.jupiter.api.Assertions.assertEquals("route-a", primary.firstValue)
    }

    @Test
    fun `POST - 仅提供primary_route_id - 服务层收到单元素集合`() {
        whenever(tripService.createTrip(any(), any(), any())).thenReturn(trip("route-a"))
        whenever(tripService.getRouteIds("trip-1")).thenReturn(listOf("route-a"))

        mockMvc.perform(
            post("/api/v1/legacy/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "夏季徒步之旅", "primary_route_id": "route-a"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.route_ids[0]").value("route-a"))

        verify(tripService).createTrip(any(), eq(listOf("route-a")), eq("route-a"))
    }

    @Test
    fun `POST - 多路线缺省主路线 - 服务层收到首个元素作为主路线`() {
        whenever(tripService.createTrip(any(), any(), any())).thenReturn(trip("route-a"))
        whenever(tripService.getRouteIds("trip-1")).thenReturn(listOf("route-a", "route-b"))

        mockMvc.perform(
            post("/api/v1/legacy/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "夏季徒步之旅", "route_ids": ["route-a", "route-b"]}""")
        )
            .andExpect(status().isCreated)

        verify(tripService).createTrip(any(), eq(listOf("route-a", "route-b")), eq("route-a"))
    }

    // ========== 创建：校验失败不产生任何写入 ==========

    @Test
    fun `POST - 主路线不在route_ids中 - 返回400且不调用服务层`() {
        mockMvc.perform(
            post("/api/v1/legacy/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "夏季徒步之旅",
                      "route_ids": ["route-a"],
                      "primary_route_id": "route-x"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)
            // 机器可读 code 位于 data.error_code，信封上的 code 是 HTTP 状态码
            .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.data.details.field").value("primary_route_id"))
            .andExpect(jsonPath("$.message").exists())

        verify(tripService, never()).createTrip(any(), any(), any())
    }

    @Test
    fun `POST - 未提供任何路线 - 返回400且不调用服务层`() {
        mockMvc.perform(
            post("/api/v1/legacy/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "夏季徒步之旅"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.data.details.field").value("route_ids"))

        verify(tripService, never()).createTrip(any(), any(), any())
    }

    @Test
    fun `POST - 显式传入空route_ids - 返回400且不调用服务层`() {
        mockMvc.perform(
            post("/api/v1/legacy/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "夏季徒步之旅", "route_ids": []}""")
        )
            .andExpect(status().isBadRequest)

        verify(tripService, never()).createTrip(any(), any(), any())
    }

    // ========== 读取：响应来自关联事实 ==========

    @Test
    fun `GET - 行程详情 - route_ids来自关联记录而非primary_route_id`() {
        whenever(tripService.getTripById("trip-1")).thenReturn(trip("route-a"))
        whenever(equipmentListRepository.findByTripId("trip-1")).thenReturn(emptyList())
        whenever(tripService.getRouteIds("trip-1")).thenReturn(listOf("route-a", "route-b", "route-c"))

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.route_ids.length()").value(3))
            .andExpect(jsonPath("$.data.route_ids[1]").value("route-b"))
    }

    @Test
    fun `GET - 历史行程详情 - 无关联记录时回退推导primary_route_id`() {
        whenever(tripService.getTripById("trip-1")).thenReturn(trip("route-legacy"))
        whenever(equipmentListRepository.findByTripId("trip-1")).thenReturn(emptyList())
        whenever(tripService.getRouteIds("trip-1")).thenReturn(emptyList())

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.route_ids.length()").value(1))
            .andExpect(jsonPath("$.data.route_ids[0]").value("route-legacy"))
    }
}
