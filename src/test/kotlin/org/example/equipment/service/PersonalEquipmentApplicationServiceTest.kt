package org.example.equipment.service

import org.example.common.contract.ApiContractException
import org.example.equipment.model.EquipmentListMemberId
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

    @Test
    fun `normalizes common whitespace and folds ASCII only within one account`() {
        val created = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("  Tent\u3000 \tBag  ", 2, 500)
        )

        assertEquals("Tent Bag", created.item.name)
        assertEquals(1000, created.summary.knownTotalWeight.grams)

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
            CreatePersonalEquipmentCommand("帐篷", 3, 700)
        )
        service.createEquipment("account-a", CreatePersonalEquipmentCommand("登山杖", 1, null))
        service.createEquipmentList("account-a", "三日清单")

        val preserved = service.updateEquipment(
            "account-a",
            created.item.identity,
            UpdatePersonalEquipmentCommand(ownedQuantity = 4)
        )
        assertEquals(700, preserved.item.unitWeight?.grams)
        assertEquals(2800, preserved.summary.knownTotalWeight.grams)
        assertEquals(1, preserved.summary.missingWeightItemCount)
        assertEquals(1, preserved.summary.equipmentListCount)

        val cleared = service.updateEquipment(
            "account-a",
            created.item.identity,
            UpdatePersonalEquipmentCommand(clearUnitWeight = true)
        )
        assertNull(cleared.item.unitWeight)
        assertEquals(0, cleared.summary.knownTotalWeight.grams)
        assertEquals(2, cleared.summary.missingWeightItemCount)
    }

    @Test
    fun `list member upsert uses relation quantity and removal preserves personal equipment`() {
        val equipment = service.createEquipment(
            "account-a",
            CreatePersonalEquipmentCommand("水壶", 5, 250)
        ).item
        val list = service.createEquipmentList("account-a", "周末").equipmentList

        val inserted = service.putListMember("account-a", list.identity, equipment.identity, 2).equipmentList
        assertEquals(1, inserted.summary.itemCount)
        assertEquals(500, inserted.summary.knownTotalWeight.grams)
        assertEquals(2, inserted.members.single().quantity)
        assertEquals(5, inserted.members.single().ownedQuantity)

        val replaced = service.putListMember("account-a", list.identity, equipment.identity, 4).equipmentList
        assertEquals(1, replaced.summary.itemCount)
        assertEquals(1000, replaced.summary.knownTotalWeight.grams)
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
            CreatePersonalEquipmentCommand("头灯", 1, 120)
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
            CreatePersonalEquipmentCommand("炉头", 1, 90)
        ).item
        val list = service.createEquipmentList("account-a", "炊事").equipmentList
        service.putListMember("account-a", list.identity, equipment.identity, 1)

        val result = service.deleteEquipmentList("account-a", list.identity)

        assertEquals(list.identity, result.deletedEquipmentListId)
        assertFalse(listRepository.existsById(list.identity))
        assertFalse(listOwnershipRepository.existsById(list.identity))
        assertTrue(equipmentRepository.existsById(equipment.identity))
    }
}
