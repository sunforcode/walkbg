package org.example.trip.service

import org.example.trip.model.Trip
import org.example.trip.model.TripRouteAssociation
import org.example.trip.repository.TripParticipantRepository
import org.example.trip.repository.TripRepository
import org.example.trip.repository.TripRouteAssociationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 覆盖 route-to-trip-linkage change 中行程与路线的关联落库行为。
 *
 * 对应 spec: trip-route-linkage
 * - Requirement: 行程与路线的关联必须作为后端事实持久化
 * - Requirement: 主路线必须是行程路线集合中的一员
 * - Requirement: 更新行程时关联事实保持一致
 */
class TripServiceRouteLinkageTest {

    private lateinit var tripRepository: TripRepository
    private lateinit var tripParticipantRepository: TripParticipantRepository
    private lateinit var associationRepository: TripRouteAssociationRepository
    private lateinit var service: TripServiceImpl

    @BeforeEach
    fun setUp() {
        tripRepository = mock()
        tripParticipantRepository = mock()
        associationRepository = mock()
        service = TripServiceImpl(tripRepository, tripParticipantRepository, associationRepository)

        // createTrip / updateTrip 内部会 copy 后落库，这里回显入参即可
        whenever(tripRepository.save(any<Trip>())).thenAnswer { it.arguments[0] as Trip }
    }

    private fun trip() = Trip(
        id = "ignored-by-service",
        name = "夏季徒步之旅",
        organizerId = "user-1",
        status = 0
    )

    @Suppress("UNCHECKED_CAST")
    private fun captureSavedAssociations(): List<TripRouteAssociation> {
        val captor = argumentCaptor<List<TripRouteAssociation>>()
        verify(associationRepository).saveAll(captor.capture())
        return captor.firstValue
    }

    // ========== 创建行程：关联记录落库 ==========

    @Test
    fun `创建单路线行程时应落库一条关联记录且标记为主路线`() {
        val created = service.createTrip(trip(), listOf("route-a"), "route-a")

        val associations = captureSavedAssociations()
        assertEquals(1, associations.size)
        assertEquals(created.id, associations[0].tripId)
        assertEquals("route-a", associations[0].routeId)
        assertTrue(associations[0].isPrimary)
        assertEquals("route-a", created.primaryRouteId)
    }

    @Test
    fun `创建多路线行程时每条路线各落库一条关联记录且仅主路线被标记`() {
        val created = service.createTrip(trip(), listOf("route-a", "route-b", "route-c"), "route-b")

        val associations = captureSavedAssociations()
        assertEquals(3, associations.size)
        assertEquals(listOf("route-a", "route-b", "route-c"), associations.map { it.routeId })
        assertTrue(associations.all { it.tripId == created.id })
        assertEquals(listOf("route-b"), associations.filter { it.isPrimary }.map { it.routeId })
        assertEquals("route-b", created.primaryRouteId)
    }

    @Test
    fun `创建行程时主路线以显式入参为准而非行程对象上的旧值`() {
        val created = service.createTrip(trip().copy(primaryRouteId = "stale-route"), listOf("route-a"), "route-a")

        assertEquals("route-a", created.primaryRouteId)
    }

    @Test
    fun `关联写入失败时异常向上抛出以触发事务回滚`() {
        whenever(associationRepository.saveAll(any<List<TripRouteAssociation>>()))
            .thenThrow(RuntimeException("association write failed"))

        assertThrows<RuntimeException> {
            service.createTrip(trip(), listOf("route-a"), "route-a")
        }
    }

    // ========== 读取：关联事实回查 ==========

    @Test
    fun `getRouteIds应把主路线排在首位`() {
        whenever(associationRepository.findByTripId("trip-1")).thenReturn(
            listOf(
                TripRouteAssociation("trip-1", "route-a", isPrimary = false),
                TripRouteAssociation("trip-1", "route-b", isPrimary = true),
                TripRouteAssociation("trip-1", "route-c", isPrimary = false)
            )
        )

        assertEquals(listOf("route-b", "route-a", "route-c"), service.getRouteIds("trip-1"))
    }

    @Test
    fun `getRouteIds在无关联记录时返回空集合`() {
        whenever(associationRepository.findByTripId("trip-1")).thenReturn(emptyList())

        assertEquals(emptyList<String>(), service.getRouteIds("trip-1"))
    }

    @Test
    fun `批量查询按行程分组且各自主路线在首位`() {
        whenever(associationRepository.findByTripIdIn(listOf("trip-1", "trip-2"))).thenReturn(
            listOf(
                TripRouteAssociation("trip-1", "route-a", isPrimary = false),
                TripRouteAssociation("trip-1", "route-b", isPrimary = true),
                TripRouteAssociation("trip-2", "route-c", isPrimary = true)
            )
        )

        val result = service.getRouteIdsByTripIds(listOf("trip-1", "trip-2"))

        assertEquals(listOf("route-b", "route-a"), result["trip-1"])
        assertEquals(listOf("route-c"), result["trip-2"])
    }

    @Test
    fun `批量查询传入空集合时不触发数据库查询`() {
        val result = service.getRouteIdsByTripIds(emptyList())

        assertEquals(emptyMap<String, List<String>>(), result)
        verify(associationRepository, never()).findByTripIdIn(any())
    }

    // ========== 更新行程：主路线同步 ==========

    @Test
    fun `更新主路线时应把旧主路线降级并把新主路线升级且集合无增减`() {
        whenever(tripRepository.existsById("trip-1")).thenReturn(true)
        val old = TripRouteAssociation("trip-1", "route-a", isPrimary = true)
        val target = TripRouteAssociation("trip-1", "route-b", isPrimary = false)
        whenever(associationRepository.findByTripId("trip-1")).thenReturn(listOf(old, target))

        service.updateTrip("trip-1", trip(), "route-b")

        val saved = captureSavedAssociations()
        assertEquals(setOf("route-a", "route-b"), saved.map { it.routeId }.toSet())
        assertFalse(old.isPrimary)
        assertTrue(target.isPrimary)
        // 集合本身不应新增成员
        verify(associationRepository, never()).save(any<TripRouteAssociation>())
    }

    @Test
    fun `新主路线尚未关联时应补写一条主路线关联记录`() {
        whenever(tripRepository.existsById("trip-1")).thenReturn(true)
        val old = TripRouteAssociation("trip-1", "route-a", isPrimary = true)
        whenever(associationRepository.findByTripId("trip-1")).thenReturn(listOf(old))

        service.updateTrip("trip-1", trip(), "route-new")

        val captor = argumentCaptor<TripRouteAssociation>()
        verify(associationRepository).save(captor.capture())
        assertEquals("route-new", captor.firstValue.routeId)
        assertTrue(captor.firstValue.isPrimary)
        assertFalse(old.isPrimary)
    }

    @Test
    fun `主路线未变化时不产生任何关联写入`() {
        whenever(tripRepository.existsById("trip-1")).thenReturn(true)
        val current = TripRouteAssociation("trip-1", "route-a", isPrimary = true)
        whenever(associationRepository.findByTripId("trip-1")).thenReturn(listOf(current))

        service.updateTrip("trip-1", trip(), "route-a")

        verify(associationRepository, never()).saveAll(any<List<TripRouteAssociation>>())
        verify(associationRepository, never()).save(any<TripRouteAssociation>())
        assertTrue(current.isPrimary)
    }

    @Test
    fun `更新未提供主路线时不触碰关联表`() {
        whenever(tripRepository.existsById("trip-1")).thenReturn(true)

        service.updateTrip("trip-1", trip(), null)

        verify(associationRepository, never()).findByTripId(any<String>())
        verify(associationRepository, never()).saveAll(any<List<TripRouteAssociation>>())
        verify(associationRepository, never()).save(any<TripRouteAssociation>())
    }

    @Test
    fun `行程不存在时更新返回null且不触碰关联表`() {
        whenever(tripRepository.existsById("missing")).thenReturn(false)

        assertEquals(null, service.updateTrip("missing", trip(), "route-a"))
        verify(associationRepository, never()).findByTripId(any<String>())
    }
}
