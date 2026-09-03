package org.example.trip.controller

import org.example.equipment.model.EquipmentList
import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.service.EquipmentListItemService
import org.example.security.JwtAuthenticationFilter
import org.example.security.JwtTokenUtil
import org.example.trip.model.Trip
import org.example.trip.service.TripService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * 覆盖 trip-equipment-link change 中新增的:
 * - TripBasicResponse.equipment_list_id 解析行为（详情接口）
 * - 新增的 GET /api/v1/legacy/trips/{id}/equipment-lists 接口
 */
@WebMvcTest(TripController::class)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

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

    private fun buildTrip(id: String = "trip-1"): Trip {
        return Trip(
            id = id,
            name = "夏季徒步之旅",
            description = "测试行程",
            organizerId = "user-1",
            status = 0
        )
    }

    private fun buildEquipmentList(id: String, tripId: String, createdAt: Instant): EquipmentList {
        return EquipmentList(
            id = id,
            name = "清单-$id",
            type = 0,
            tripId = tripId,
            creatorId = "user-1",
            personCount = 1,
            status = 0,
            createdAt = createdAt
        )
    }

    // ========== GET /api/v1/legacy/trips/{id} - equipment_list_id 解析 ==========

    @Test
    fun `GET - trip详情 - 无关联装备清单时equipment_list_id为null`() {
        val trip = buildTrip("trip-1")
        whenever(tripService.getTripById("trip-1")).thenReturn(trip)
        whenever(equipmentListRepository.findByTripId("trip-1")).thenReturn(emptyList())

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.equipment_list_id").doesNotExist())
    }

    @Test
    fun `GET - trip详情 - 存在单个关联装备清单时应返回其id`() {
        val trip = buildTrip("trip-1")
        val list = buildEquipmentList("list-1", "trip-1", Instant.now())
        whenever(tripService.getTripById("trip-1")).thenReturn(trip)
        whenever(equipmentListRepository.findByTripId("trip-1")).thenReturn(listOf(list))

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.equipment_list_id").value("list-1"))
    }

    @Test
    fun `GET - trip详情 - 存在多个关联装备清单时应返回最近创建的一个`() {
        val trip = buildTrip("trip-1")
        val older = buildEquipmentList("list-old", "trip-1", Instant.now().minusSeconds(3600))
        val newer = buildEquipmentList("list-new", "trip-1", Instant.now())
        whenever(tripService.getTripById("trip-1")).thenReturn(trip)
        whenever(equipmentListRepository.findByTripId("trip-1")).thenReturn(listOf(older, newer))

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.equipment_list_id").value("list-new"))
    }

    // ========== GET /api/v1/legacy/trips - 列表接口不解析 equipment_list_id（避免N+1） ==========

    @Test
    fun `GET - trip列表 - equipment_list_id始终为null且不触发装备清单查询`() {
        val trip = buildTrip("trip-1")
        val page = PageImpl(listOf(trip), PageRequest.of(0, 10), 1)
        whenever(tripService.getAllTrips(any())).thenReturn(page)

        mockMvc.perform(get("/api/v1/legacy/trips"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].equipment_list_id").doesNotExist())
    }

    // ========== GET /api/v1/legacy/trips/{id}/equipment-lists ==========

    @Test
    fun `GET - trip装备清单列表 - 存在多个清单时应全部返回`() {
        val trip = buildTrip("trip-1")
        val list1 = buildEquipmentList("list-1", "trip-1", Instant.now().minusSeconds(60))
        val list2 = buildEquipmentList("list-2", "trip-1", Instant.now())
        val page = PageImpl(listOf(list1, list2), PageRequest.of(0, 10), 2)

        whenever(tripService.getTripById("trip-1")).thenReturn(trip)
        whenever(equipmentListRepository.findByTripId(eq("trip-1"), any())).thenReturn(page)
        whenever(equipmentListItemService.countListItems(any())).thenReturn(0L)

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1/equipment-lists"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].id").value("list-1"))
            .andExpect(jsonPath("$.data.content[1].id").value("list-2"))
    }

    @Test
    fun `GET - trip装备清单列表 - 无关联清单时返回空分页`() {
        val trip = buildTrip("trip-1")
        val page = PageImpl<EquipmentList>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(tripService.getTripById("trip-1")).thenReturn(trip)
        whenever(equipmentListRepository.findByTripId(eq("trip-1"), any())).thenReturn(page)

        mockMvc.perform(get("/api/v1/legacy/trips/trip-1/equipment-lists"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(0))
    }

    @Test
    fun `GET - trip装备清单列表 - 行程不存在时返回404`() {
        whenever(tripService.getTripById("missing-trip")).thenReturn(null)

        mockMvc.perform(get("/api/v1/legacy/trips/missing-trip/equipment-lists"))
            .andExpect(status().isNotFound)
    }
}
