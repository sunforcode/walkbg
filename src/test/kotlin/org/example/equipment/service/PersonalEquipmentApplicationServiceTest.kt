package org.example.equipment.service

import org.example.common.contract.ApiContractException
import org.example.equipment.model.EquipmentListMemberId
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.model.PersonalEquipmentOwnership
import org.example.equipment.catalog.model.CatalogEquipmentRecord
import org.example.equipment.catalog.model.CatalogSpecRecord
import org.example.equipment.catalog.service.EquipmentCatalogApplicationService
import org.example.equipment.support.EquipmentCommandStore
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.mock.mockito.MockBean
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.example.equipment.repository.EquipmentListMemberRepository
import org.example.equipment.repository.EquipmentListOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.repository.UserEquipmentListRecordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
    ]
)
@Import(PersonalEquipmentApplicationService::class, PersonalEquipmentDomainService::class)
class PersonalEquipmentApplicationServiceTest {
    @Autowired
    private lateinit var service: PersonalEquipmentApplicationService

    @Autowired
    private lateinit var equipmentRepository: PersonalEquipmentRecordRepository

    @Autowired
    private lateinit var equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository

    @Autowired
    private lateinit var listRepository: UserEquipmentListRecordRepository

    @Autowired
    private lateinit var listOwnershipRepository: EquipmentListOwnershipRepository

    @Autowired
    private lateinit var memberRepository: EquipmentListMemberRepository

    @MockBean private lateinit var commandStore: EquipmentCommandStore
    @MockBean private lateinit var catalogService: EquipmentCatalogApplicationService

    @BeforeEach
    fun executeCommands() {
        doAnswer { invocation -> invocation.getArgument<() -> Any>(5).invoke() }
            .whenever(commandStore).execute(any(), any(), any(), any(), any<Class<Any>>(), any())
    }

    @Test
    fun `normalizes common whitespace and folds ASCII only within one account`() {
        val created = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("  Tent\u3000 \tBag  ", 2, 500.0)
        )

        assertEquals("Tent Bag", created.item.name)
        assertEquals(1000.0, created.summary.knownTotalWeight.grams)

        val conflict = assertThrows<ApiContractException> {
            service.createEquipment("account-a", CreatePersonalEquipmentCommand("tent bag", 1, null))
        }
        assertEquals("equipment_name_conflict", conflict.code)

        val otherAccount = service.createEquipment(
            "account-b",
            CreatePersonalEquipmentCommand("TENT BAG", 1, null)
        )
        assertEquals("TENT BAG", otherAccount.item.name)

        service.createEquipment("account-a", CreatePersonalEquipmentCommand("Ä", 1, null))
        service.createEquipment("account-a", CreatePersonalEquipmentCommand("ä", 1, null))
        assertEquals(3, service.getEquipmentCollection("account-a").items.size)
    }

    @Test
    fun `patch preserves missing fields and explicit null clears unit weight`() {
        val created = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("帐篷", 3, 700.0)
        )
        service.createEquipment("account-a", CreatePersonalEquipmentCommand("登山杖", 1, null))
        service.createEquipmentList("account-a", "三日清单")

        val preserved = service.updateEquipment(
            "account-a",
            created.item.identity,
            UpdatePersonalEquipmentCommand(ownedQuantity = 4)
        )
        assertEquals(700.0, preserved.item.unitWeight?.grams)
        assertEquals(2800.0, preserved.summary.knownTotalWeight.grams)
        assertEquals(1, preserved.summary.missingWeightItemCount)
        assertEquals(1, preserved.summary.equipmentListCount)

        val cleared = service.updateEquipment(
            "account-a",
            created.item.identity,
            UpdatePersonalEquipmentCommand(clearUnitWeight = true)
        )
        assertNull(cleared.item.unitWeight)
        assertEquals(0.0, cleared.summary.knownTotalWeight.grams)
        assertEquals(2, cleared.summary.missingWeightItemCount)
    }

    @Test
    fun `list member upsert uses relation quantity and removal preserves personal equipment`() {
        val equipment = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("水壶", 5, 250.0)
        ).item
        val list = service.createEquipmentList("account-a", "周末").equipmentList

        val inserted = service.putListMember("account-a", list.identity, equipment.identity, 2).equipmentList
        assertEquals(1, inserted.summary.itemCount)
        assertEquals(500.0, inserted.summary.knownTotalWeight.grams)
        assertEquals(2, inserted.members.single().quantity)
        assertEquals(5, inserted.members.single().ownedQuantity)

        val replaced = service.putListMember("account-a", list.identity, equipment.identity, 4).equipmentList
        assertEquals(1, replaced.summary.itemCount)
        assertEquals(1000.0, replaced.summary.knownTotalWeight.grams)
        assertEquals(4, replaced.members.single().quantity)

        val renamed = service.renameEquipmentList("account-a", list.identity, "过夜").equipmentList
        assertEquals("过夜", renamed.name)

        val removed = service.removeListMember("account-a", list.identity, equipment.identity).equipmentList
        assertTrue(removed.members.isEmpty())
        assertTrue(equipmentRepository.existsById(equipment.identity))

        val missing = assertThrows<ApiContractException> {
            service.removeListMember("account-a", list.identity, equipment.identity)
        }
        assertEquals("resource_not_found", missing.code)
    }

    @Test
    fun `delete requires confirmation and atomically removes all memberships but keeps lists`() {
        val equipment = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("睡袋", 1, null)
        ).item
        val first = service.createEquipmentList("account-a", "一日").equipmentList
        val second = service.createEquipmentList("account-a", "三日").equipmentList
        service.putListMember("account-a", first.identity, equipment.identity, 1)
        service.putListMember("account-a", second.identity, equipment.identity, 1)

        val impact = service.getDeletionImpact("account-a", equipment.identity)
        assertEquals(2, impact.equipmentListReferenceCount)
        assertEquals(setOf(first.identity, second.identity), impact.affectedEquipmentLists.map { it.identity }.toSet())

        val confirmation = assertThrows<ApiContractException> {
            service.deleteEquipment("account-a", equipment.identity, confirmListRemoval = false)
        }
        assertEquals("deletion_confirmation_required", confirmation.code)
        assertTrue(equipmentRepository.existsById(equipment.identity))

        val deleted = service.deleteEquipment("account-a", equipment.identity, confirmListRemoval = true)
        assertEquals(2, deleted.removedEquipmentListReferenceCount)
        assertFalse(equipmentRepository.existsById(equipment.identity))
        assertFalse(equipmentOwnershipRepository.existsById(equipment.identity))
        assertFalse(memberRepository.existsById(EquipmentListMemberId(first.identity, equipment.identity)))
        assertFalse(memberRepository.existsById(EquipmentListMemberId(second.identity, equipment.identity)))
        assertTrue(listRepository.existsById(first.identity))
        assertTrue(listRepository.existsById(second.identity))
    }

    @Test
    fun `resource reads and member writes are isolated by authenticated account`() {
        val equipment = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("头灯", 1, 120.0)
        ).item
        val ownList = service.createEquipmentList("account-a", "夜行").equipmentList
        val otherList = service.createEquipmentList("account-b", "他人清单").equipmentList

        assertEquals("resource_not_found", assertThrows<ApiContractException> {
            service.getEquipment("account-b", equipment.identity)
        }.code)
        assertEquals("resource_not_found", assertThrows<ApiContractException> {
            service.putListMember("account-b", otherList.identity, equipment.identity, 1)
        }.code)
        assertEquals("resource_not_found", assertThrows<ApiContractException> {
            service.getEquipmentList("account-b", ownList.identity)
        }.code)
    }

    @Test
    fun `deleting a list removes memberships but preserves equipment`() {
        val equipment = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("炉头", 1, 90.0)
        ).item
        val list = service.createEquipmentList("account-a", "炊事").equipmentList
        service.putListMember("account-a", list.identity, equipment.identity, 1)

        val result = service.deleteEquipmentList("account-a", list.identity)

        assertEquals(list.identity, result.deletedEquipmentListId)
        assertFalse(listRepository.existsById(list.identity))
        assertFalse(listOwnershipRepository.existsById(list.identity))
        assertTrue(equipmentRepository.existsById(equipment.identity))
    }
    @Test
    fun `filters use literal ASCII normalized names and leave whole account summary unchanged`() {
        service.createEquipment("account-a", CreatePersonalEquipmentCommand(" TENT_50% ", 2, 12.5, "tent", "tent"))
        service.createEquipment("account-a", CreatePersonalEquipmentCommand("气垫", 1, null, "sleeping", "sleeping_pad"))
        service.createEquipment("account-b", CreatePersonalEquipmentCommand("他人", 1, 999.0))
        service.createEquipmentList("account-a", "周末")
        val filtered = service.getEquipmentCollection("account-a", PersonalEquipmentFilter(category = "sleeping", missingWeight = true))
        assertEquals(listOf("气垫"), filtered.items.map { it.name })
        assertEquals(2, filtered.summary.equipmentItemCount)
        assertEquals(1, filtered.summary.equipmentListCount)
        assertEquals(25.0, filtered.summary.knownTotalWeight.grams)
        assertEquals(1, filtered.summary.missingWeightItemCount)
        assertEquals("TENT_50%", service.getEquipmentCollection("account-a", PersonalEquipmentFilter(keyword = "  tent_50%  ", missingWeight = false)).items.single().name)
        val empty = service.getEquipmentCollection("account-a", PersonalEquipmentFilter(keyword = "absent"))
        assertTrue(empty.items.isEmpty())
        assertEquals(filtered.summary, empty.summary)
        listOf(PersonalEquipmentFilter(category = "Sleeping"), PersonalEquipmentFilter(equipmentType = "bad"),
            PersonalEquipmentFilter(category = "tent", equipmentType = "sleeping_bag"), PersonalEquipmentFilter(keyword = "\u3000\t")).forEach {
            assertEquals(400, assertThrows<ApiContractException> { service.getEquipmentCollection("account-a", it) }.status.value())
        }
    }

    @Test
    fun `new configuration records decimal user weights while old unknown metadata stays absent`() {
        val item = service.createEquipment("account-a", CreatePersonalEquipmentCommand("袜子", 2, 12.5, "clothing", "socks", " M ", "双", " 含两只 ")).item
        assertEquals(12.5, item.unitWeight?.grams)
        assertEquals("user_recorded", item.unitWeightSource)
        assertEquals("M", item.variant)
        assertEquals("含两只", item.weightScope)
        equipmentRepository.save(PersonalEquipmentRecord("legacy", "旧装备", 1, 9.5))
        equipmentOwnershipRepository.save(PersonalEquipmentOwnership("legacy", "account-a", "旧装备"))
        val legacy = service.getEquipment("account-a", "legacy")
        assertNull(legacy.category); assertNull(legacy.unitWeightSource); assertNull(legacy.unitLabel)
        assertEquals(9.5, legacy.unitWeight?.grams)
    }

    @Test
    fun `patch validates final configuration and reference changes before any mutation`() {
        val item = service.createEquipment("account-a", CreatePersonalEquipmentCommand("原袜子", 1, 12.5, "clothing", "socks", unitLabel = "双")).item
        val invalid = listOf(
            UpdatePersonalEquipmentCommand(name = "不应保存", unitLabel = "只"),
            UpdatePersonalEquipmentCommand(name = "不应保存", category = "tent"),
            UpdatePersonalEquipmentCommand(name = "不应保存", hasCategory = true),
            UpdatePersonalEquipmentCommand(name = "不应保存", unitWeightGrams = -1.0)
        )
        invalid.forEach {
            assertEquals(422, assertThrows<ApiContractException> { service.updateEquipment("account-a", item.identity, it) }.status.value())
            assertEquals(item, service.getEquipment("account-a", item.identity))
        }
        val changed = service.updateEquipment("account-a", item.identity, UpdatePersonalEquipmentCommand(unitLabel = "只", unitWeightGrams = 6.25)).item
        assertEquals("只", changed.unitLabel)
        assertEquals(6.25, changed.unitWeight?.grams)
        val cleared = service.updateEquipment("account-a", item.identity, UpdatePersonalEquipmentCommand(hasCategory = true, hasEquipmentType = true)).item
        assertNull(cleared.category); assertNull(cleared.equipmentType)
    }

    @Test
    fun `catalog adoption explicitly copies one reference and keeps independent own configuration`() {
        val source = source()
        whenever(catalogService.getRecord("catalog-1")).thenReturn(source)
        val adopted = service.adoptCatalogEquipment("account-a", "adopt", AdoptCatalogEquipmentCommand("catalog-1", 2)).item
        assertEquals(12.5, adopted.unitWeight?.grams)
        assertEquals("catalog_reference", adopted.unitWeightSource)
        assertEquals("catalog-1", adopted.catalogEquipmentId)
        assertEquals("M", adopted.variant)
        assertEquals("双", adopted.unitLabel)
        whenever(catalogService.getRecord("catalog-1")).thenReturn(source.copy(name = "目录已修改", specifications = source.specifications!!.copy(unitWeightGrams = 99.0)))
        assertEquals(adopted, service.getEquipment("account-a", adopted.identity))
        val rejected = listOf(
            UpdatePersonalEquipmentCommand(variant = "L"), UpdatePersonalEquipmentCommand(weightScope = "一只"),
            UpdatePersonalEquipmentCommand(category = "clothing", equipmentType = "gloves"),
            UpdatePersonalEquipmentCommand(hasCategory = true, hasEquipmentType = true)
        )
        rejected.forEach {
            assertEquals(422, assertThrows<ApiContractException> { service.updateEquipment("account-a", adopted.identity, it) }.status.value())
            assertEquals(adopted, service.getEquipment("account-a", adopted.identity))
        }
        val own = service.updateEquipment("account-a", adopted.identity, UpdatePersonalEquipmentCommand(weightScope = "我自己的整双", unitWeightGrams = 13.5)).item
        assertEquals("user_recorded", own.unitWeightSource)
        assertEquals("catalog-1", own.catalogEquipmentId)
        assertEquals("另一说明", service.updateEquipment("account-a", own.identity, UpdatePersonalEquipmentCommand(weightScope = "另一说明")).item.weightScope)
    }

    @Test
    fun `catalog adoption allows absent specs explicit unknown and explicit user override only`() {
        whenever(catalogService.getRecord("catalog-1")).thenReturn(source())
        assertEquals(422, assertThrows<ApiContractException> {
            service.adoptCatalogEquipment("account-a", "invalid", AdoptCatalogEquipmentCommand("catalog-1", 1, variant = "L"))
        }.status.value())
        assertEquals(0, service.getEquipmentCollection("account-a").summary.equipmentItemCount)
        val unknown = service.adoptCatalogEquipment("account-a", "unknown", AdoptCatalogEquipmentCommand("catalog-1", 1, hasUnitWeight = true, variant = "L")).item
        assertNull(unknown.unitWeight); assertNull(unknown.unitWeightSource)
        val measured = service.adoptCatalogEquipment("account-a", "measured", AdoptCatalogEquipmentCommand("catalog-1", 1, name = "称重袜子", unitWeightGrams = 13.75, variant = "L")).item
        assertEquals(13.75, measured.unitWeight?.grams); assertEquals("user_recorded", measured.unitWeightSource)
        whenever(catalogService.getRecord("catalog-empty")).thenReturn(source().copy(identity = "catalog-empty", name = "无规格袜子", specifications = null))
        val noSpec = service.adoptCatalogEquipment("account-a", "empty", AdoptCatalogEquipmentCommand("catalog-empty", 1)).item
        assertNull(noSpec.unitLabel); assertNull(noSpec.unitWeight); assertNull(noSpec.weightScope)
        assertEquals("socks", noSpec.equipmentType)
    }

    @Test
    fun `copy has new list identity and independent quantities but references existing assets`() {
        whenever(catalogService.getRecord("catalog-1")).thenReturn(source())
        val equipment = service.adoptCatalogEquipment("account-a", "copy-asset", AdoptCatalogEquipmentCommand("catalog-1", 5)).item
        val source = service.createEquipmentList("account-a", "原清单").equipmentList
        service.putListMember("account-a", source.identity, equipment.identity, 2)
        val copied = service.copyEquipmentList("account-a", source.identity, "copy", "新清单")
        assertTrue(copied.equipmentList.identity != source.identity)
        assertEquals(equipment.identity, copied.equipmentList.members.single().identity)
        assertEquals(2, copied.equipmentList.members.single().quantity)
        val member = copied.equipmentList.members.single()
        assertEquals("clothing", member.category); assertEquals("socks", member.equipmentType)
        assertEquals("M", member.variant); assertEquals("双", member.unitLabel); assertEquals("一双", member.weightScope)
        assertEquals("catalog_reference", member.unitWeightSource); assertEquals("catalog-1", member.catalogEquipmentId)
        assertEquals(25.0, copied.equipmentList.summary.knownTotalWeight.grams)
        assertEquals(1, copied.personalEquipmentSummary.equipmentItemCount)
        assertEquals(2, copied.personalEquipmentSummary.equipmentListCount)
        service.putListMember("account-a", copied.equipmentList.identity, equipment.identity, 1)
        assertEquals(2, service.getEquipmentList("account-a", source.identity).members.single().quantity)
        service.updateEquipment("account-a", equipment.identity, UpdatePersonalEquipmentCommand(name = "改名袜子"))
        assertEquals("改名袜子", service.getEquipmentList("account-a", copied.equipmentList.identity).members.single().name)
        assertEquals("resource_not_found", assertThrows<ApiContractException> {
            service.copyEquipmentList("account-b", source.identity, "other", "他人")
        }.code)
        val empty = service.createEquipmentList("account-a", "空").equipmentList
        assertTrue(service.copyEquipmentList("account-a", empty.identity, "empty", "空副本").equipmentList.members.isEmpty())
    }

    private fun source() = CatalogEquipmentRecord("catalog-1", "参考袜子", "clothing", "socks",
        specifications = CatalogSpecRecord(variant = "M", unitLabel = "双", unitWeightGrams = 12.5, weightScope = "一双"))

}
