package org.example.equipment.service

import org.example.common.BaseUnitTest
import org.example.equipment.model.EquipmentList
import org.example.equipment.repository.EquipmentItemRepository
import org.example.equipment.repository.EquipmentListItemRepository
import org.example.equipment.repository.EquipmentListRepository
import org.example.equipment.repository.EquipmentTemplateRepository
import org.example.user.repository.UserEquipmentItemRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

/**
 * 覆盖 trip-equipment-link change 中新增的 tripId 创建/更新行为，
 * 以及此前已修复的 type/status Int-String 解析 bug。
 */
class EquipmentServiceImplTest : BaseUnitTest() {

    @Mock
    private lateinit var equipmentListRepository: EquipmentListRepository

    @Mock
    private lateinit var equipmentItemRepository: EquipmentItemRepository

    @Mock
    private lateinit var equipmentListItemRepository: EquipmentListItemRepository

    @Mock
    private lateinit var userEquipmentItemRepository: UserEquipmentItemRepository

    @Mock
    private lateinit var equipmentTemplateRepository: EquipmentTemplateRepository

    @InjectMocks
    private lateinit var equipmentService: EquipmentServiceImpl

    // ========== createEquipmentList - tripId ==========

    @Test
    fun `createEquipmentList - 提供tripId时应持久化到实体`() {
        whenever(equipmentListRepository.save(any<EquipmentList>())).thenAnswer { it.arguments[0] }

        val request = mapOf<String, Any>(
            "name" to "三日徒步清单",
            "type" to 1,
            "personCount" to 3,
            "tripId" to "trip-001"
        )

        val result = equipmentService.createEquipmentList(request, "user-1", "张三")

        assertEquals("trip-001", result.tripId)
        assertEquals(1, result.type)
        assertEquals(3, result.personCount)

        val captor = argumentCaptor<EquipmentList>()
        verify(equipmentListRepository).save(captor.capture())
        assertEquals("trip-001", captor.firstValue.tripId)
    }

    @Test
    fun `createEquipmentList - 未提供tripId时应为null`() {
        whenever(equipmentListRepository.save(any<EquipmentList>())).thenAnswer { it.arguments[0] }

        val request = mapOf<String, Any>(
            "name" to "个人清单",
            "type" to 0,
            "personCount" to 1
        )

        val result = equipmentService.createEquipmentList(request, "user-1", "张三")

        assertNull(result.tripId)
    }

    @Test
    fun `createEquipmentList - type字段应正确解析为Int而非恒为0`() {
        whenever(equipmentListRepository.save(any<EquipmentList>())).thenAnswer { it.arguments[0] }

        val request = mapOf<String, Any>(
            "name" to "团队清单",
            "type" to 1,
            "personCount" to 4
        )

        val result = equipmentService.createEquipmentList(request, "user-1", "张三")

        assertEquals(1, result.type)
    }

    // ========== updateEquipmentList - tripId ==========

    @Test
    fun `updateEquipmentList - 提供tripId时应关联到指定行程`() {
        val existing = EquipmentList(
            id = "list-1",
            name = "个人清单",
            type = 0,
            tripId = null,
            creatorId = "user-1",
            personCount = 1,
            status = 0
        )
        whenever(equipmentListRepository.findById("list-1")).thenReturn(Optional.of(existing))
        whenever(equipmentListRepository.save(any<EquipmentList>())).thenAnswer { it.arguments[0] }

        val result = equipmentService.updateEquipmentList("list-1", mapOf("tripId" to "trip-002"))

        assertNotNull(result)
        assertEquals("trip-002", result!!.tripId)
    }

    @Test
    fun `updateEquipmentList - 未提供tripId时应保持原有关联不变`() {
        val existing = EquipmentList(
            id = "list-1",
            name = "个人清单",
            type = 0,
            tripId = "trip-original",
            creatorId = "user-1",
            personCount = 1,
            status = 0
        )
        whenever(equipmentListRepository.findById("list-1")).thenReturn(Optional.of(existing))
        whenever(equipmentListRepository.save(any<EquipmentList>())).thenAnswer { it.arguments[0] }

        val result = equipmentService.updateEquipmentList("list-1", mapOf("name" to "新名称"))

        assertNotNull(result)
        assertEquals("trip-original", result!!.tripId)
        assertEquals("新名称", result.name)
    }

    @Test
    fun `updateEquipmentList - status字段应正确解析为Int而非仅支持字符串`() {
        val existing = EquipmentList(
            id = "list-1",
            name = "个人清单",
            type = 0,
            tripId = null,
            creatorId = "user-1",
            personCount = 1,
            status = 0
        )
        whenever(equipmentListRepository.findById("list-1")).thenReturn(Optional.of(existing))
        whenever(equipmentListRepository.save(any<EquipmentList>())).thenAnswer { it.arguments[0] }

        // 模拟通用 PUT 接口从 JSON 反序列化出的原生 Int（而不是字符串）
        val result = equipmentService.updateEquipmentList("list-1", mapOf("status" to 2))

        assertNotNull(result)
        assertEquals(2, result!!.status)
    }
}
