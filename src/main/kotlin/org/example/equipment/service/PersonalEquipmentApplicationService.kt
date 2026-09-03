package org.example.equipment.service

import org.example.common.contract.ApiContractException
import org.example.common.util.IdGenerator
import org.example.equipment.dto.EquipmentListCollectionResponse
import org.example.equipment.dto.EquipmentListCreateResponse
import org.example.equipment.dto.EquipmentListDeletionResponse
import org.example.equipment.dto.EquipmentListDetailProjection
import org.example.equipment.dto.EquipmentListMemberProjection
import org.example.equipment.dto.EquipmentListMutationResponse
import org.example.equipment.dto.EquipmentListReferenceProjection
import org.example.equipment.dto.EquipmentListSummary
import org.example.equipment.dto.EquipmentListSummaryProjection
import org.example.equipment.dto.PersonalEquipmentCollectionResponse
import org.example.equipment.dto.PersonalEquipmentDeletionImpact
import org.example.equipment.dto.PersonalEquipmentDeletionResponse
import org.example.equipment.dto.PersonalEquipmentMutationResponse
import org.example.equipment.dto.PersonalEquipmentProjection
import org.example.equipment.dto.PersonalEquipmentSummary
import org.example.equipment.dto.WeightProjection
import org.example.equipment.model.EquipmentListMember
import org.example.equipment.model.EquipmentListMemberId
import org.example.equipment.model.EquipmentListOwnership
import org.example.equipment.model.PersonalEquipmentOwnership
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.model.UserEquipmentListRecord
import org.example.equipment.repository.EquipmentListMemberRepository
import org.example.equipment.repository.EquipmentListOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.repository.UserEquipmentListRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class CreatePersonalEquipmentCommand(
    val name: String?,
    val ownedQuantity: Int?,
    val unitWeightGrams: Long?
)

data class UpdatePersonalEquipmentCommand(
    val name: String? = null,
    val ownedQuantity: Int? = null,
    val unitWeightGrams: Long? = null,
    val clearUnitWeight: Boolean = false,
    val hasName: Boolean = name != null,
    val hasOwnedQuantity: Boolean = ownedQuantity != null,
    val hasUnitWeight: Boolean = unitWeightGrams != null || clearUnitWeight
)

@Service
class PersonalEquipmentApplicationService(
    private val equipmentRepository: PersonalEquipmentRecordRepository,
    private val equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository,
    private val listRepository: UserEquipmentListRecordRepository,
    private val listOwnershipRepository: EquipmentListOwnershipRepository,
    private val memberRepository: EquipmentListMemberRepository,
    private val domainService: PersonalEquipmentDomainService
) {
    @Transactional(readOnly = true)
    fun getEquipmentCollection(accountId: String): PersonalEquipmentCollectionResponse {
        val items = ownedEquipment(accountId).map(::toEquipmentProjection)
        return PersonalEquipmentCollectionResponse(items, personalSummary(accountId, items))
    }

    @Transactional
    fun createEquipment(accountId: String, command: CreatePersonalEquipmentCommand): PersonalEquipmentMutationResponse {
        val normalized = domainService.normalizeEquipmentName(command.name)
        val quantity = domainService.requirePositiveQuantity(command.ownedQuantity, "拥有数量")
        val unitWeight = command.unitWeightGrams?.let(domainService::requirePositiveWeight)
        ensureUniqueEquipmentName(accountId, normalized.comparison)

        val id = IdGenerator.generateIdWithPrefix("pe")
        val record = equipmentRepository.save(
            PersonalEquipmentRecord(id, normalized.display, quantity, unitWeight)
        )
        equipmentOwnershipRepository.save(
            PersonalEquipmentOwnership(id, accountId, normalized.comparison)
        )
        return PersonalEquipmentMutationResponse(toEquipmentProjection(record), personalSummary(accountId))
    }

    @Transactional(readOnly = true)
    fun getEquipment(accountId: String, personalEquipmentId: String): PersonalEquipmentProjection =
        toEquipmentProjection(requireOwnedEquipment(accountId, personalEquipmentId))

    @Transactional
    fun updateEquipment(
        accountId: String,
        personalEquipmentId: String,
        command: UpdatePersonalEquipmentCommand
    ): PersonalEquipmentMutationResponse {
        val record = requireOwnedEquipment(accountId, personalEquipmentId)
        val ownership = requireEquipmentOwnership(accountId, personalEquipmentId)
        if (command.hasName) {
            val normalized = domainService.normalizeEquipmentName(command.name)
            if (equipmentOwnershipRepository.existsByAccountIdAndNormalizedNameAndPersonalEquipmentIdNot(
                    accountId,
                    normalized.comparison,
                    personalEquipmentId
                )
            ) {
                throw ApiContractException.conflict("equipment_name_conflict", "当前账号已存在同名装备")
            }
            record.name = normalized.display
            ownership.normalizedName = normalized.comparison
        }
        if (command.hasOwnedQuantity) {
            record.ownedQuantity = domainService.requirePositiveQuantity(command.ownedQuantity, "拥有数量")
        }
        if (command.hasUnitWeight) {
            record.unitWeightGrams = if (command.clearUnitWeight) {
                null
            } else {
                domainService.requirePositiveWeight(command.unitWeightGrams)
            }
        }
        record.updatedAt = Instant.now()
        equipmentRepository.save(record)
        equipmentOwnershipRepository.save(ownership)
        return PersonalEquipmentMutationResponse(toEquipmentProjection(record), personalSummary(accountId))
    }

    @Transactional(readOnly = true)
    fun getDeletionImpact(accountId: String, personalEquipmentId: String): PersonalEquipmentDeletionImpact {
        requireOwnedEquipment(accountId, personalEquipmentId)
        val references = memberRepository.findByPersonalEquipmentId(personalEquipmentId)
            .mapNotNull { member ->
                val ownership = listOwnershipRepository.findById(member.equipmentListId).orElse(null)
                if (ownership?.accountId != accountId) return@mapNotNull null
                listRepository.findById(member.equipmentListId).orElse(null)?.let {
                    EquipmentListReferenceProjection(it.id, it.name)
                }
            }
        return PersonalEquipmentDeletionImpact(personalEquipmentId, references.size, references)
    }

    @Transactional
    fun deleteEquipment(
        accountId: String,
        personalEquipmentId: String,
        confirmListRemoval: Boolean
    ): PersonalEquipmentDeletionResponse {
        if (!confirmListRemoval) {
            throw ApiContractException.conflict(
                "deletion_confirmation_required",
                "删除装备前必须确认从所有清单移除"
            )
        }
        requireOwnedEquipment(accountId, personalEquipmentId)
        val removedCount = memberRepository.findByPersonalEquipmentId(personalEquipmentId).size
        memberRepository.deleteByPersonalEquipmentId(personalEquipmentId)
        equipmentOwnershipRepository.deleteById(personalEquipmentId)
        equipmentRepository.deleteById(personalEquipmentId)
        return PersonalEquipmentDeletionResponse(
            personalEquipmentId,
            removedCount,
            personalSummary(accountId)
        )
    }

    @Transactional(readOnly = true)
    fun getEquipmentLists(accountId: String): EquipmentListCollectionResponse = EquipmentListCollectionResponse(
        ownedLists(accountId).map { list ->
            val detail = toListDetail(list)
            EquipmentListSummaryProjection(detail.identity, detail.name, detail.summary)
        }
    )

    @Transactional
    fun createEquipmentList(accountId: String, name: String?): EquipmentListCreateResponse {
        val normalizedName = domainService.normalizeListName(name)
        val id = IdGenerator.generateIdWithPrefix("el")
        val list = listRepository.save(UserEquipmentListRecord(id, normalizedName))
        listOwnershipRepository.save(EquipmentListOwnership(id, accountId))
        return EquipmentListCreateResponse(toListDetail(list), personalSummary(accountId))
    }

    @Transactional(readOnly = true)
    fun getEquipmentList(accountId: String, equipmentListId: String): EquipmentListDetailProjection =
        toListDetail(requireOwnedList(accountId, equipmentListId))

    @Transactional
    fun renameEquipmentList(
        accountId: String,
        equipmentListId: String,
        name: String?
    ): EquipmentListMutationResponse {
        val list = requireOwnedList(accountId, equipmentListId)
        list.name = domainService.normalizeListName(name)
        list.updatedAt = Instant.now()
        return EquipmentListMutationResponse(toListDetail(listRepository.save(list)))
    }

    @Transactional
    fun deleteEquipmentList(accountId: String, equipmentListId: String): EquipmentListDeletionResponse {
        requireOwnedList(accountId, equipmentListId)
        memberRepository.deleteByEquipmentListId(equipmentListId)
        listOwnershipRepository.deleteById(equipmentListId)
        listRepository.deleteById(equipmentListId)
        return EquipmentListDeletionResponse(equipmentListId, personalSummary(accountId))
    }

    @Transactional
    fun putListMember(
        accountId: String,
        equipmentListId: String,
        personalEquipmentId: String,
        quantity: Int?
    ): EquipmentListMutationResponse {
        val list = requireOwnedList(accountId, equipmentListId)
        requireOwnedEquipment(accountId, personalEquipmentId)
        val validatedQuantity = domainService.requirePositiveQuantity(quantity, "清单数量")
        val id = EquipmentListMemberId(equipmentListId, personalEquipmentId)
        val member = memberRepository.findById(id).orElse(
            EquipmentListMember(equipmentListId, personalEquipmentId, validatedQuantity)
        )
        member.quantity = validatedQuantity
        memberRepository.save(member)
        return EquipmentListMutationResponse(toListDetail(list))
    }

    @Transactional
    fun removeListMember(
        accountId: String,
        equipmentListId: String,
        personalEquipmentId: String
    ): EquipmentListMutationResponse {
        val list = requireOwnedList(accountId, equipmentListId)
        requireOwnedEquipment(accountId, personalEquipmentId)
        val id = EquipmentListMemberId(equipmentListId, personalEquipmentId)
        if (!memberRepository.existsById(id)) throw ApiContractException.notFound()
        memberRepository.deleteById(id)
        return EquipmentListMutationResponse(toListDetail(list))
    }

    private fun ownedEquipment(accountId: String): List<PersonalEquipmentRecord> =
        equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(accountId)
            .mapNotNull { equipmentRepository.findById(it.personalEquipmentId).orElse(null) }

    private fun ownedLists(accountId: String): List<UserEquipmentListRecord> =
        listOwnershipRepository.findByAccountIdOrderByEquipmentListIdAsc(accountId)
            .mapNotNull { listRepository.findById(it.equipmentListId).orElse(null) }

    private fun requireOwnedEquipment(accountId: String, personalEquipmentId: String): PersonalEquipmentRecord {
        requireEquipmentOwnership(accountId, personalEquipmentId)
        return equipmentRepository.findById(personalEquipmentId).orElseThrow { ApiContractException.notFound() }
    }

    private fun requireEquipmentOwnership(
        accountId: String,
        personalEquipmentId: String
    ): PersonalEquipmentOwnership =
        equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId(personalEquipmentId, accountId)
            ?: throw ApiContractException.notFound()

    private fun requireOwnedList(accountId: String, equipmentListId: String): UserEquipmentListRecord {
        listOwnershipRepository.findByEquipmentListIdAndAccountId(equipmentListId, accountId)
            ?: throw ApiContractException.notFound()
        return listRepository.findById(equipmentListId).orElseThrow { ApiContractException.notFound() }
    }

    private fun ensureUniqueEquipmentName(accountId: String, normalizedName: String) {
        if (equipmentOwnershipRepository.existsByAccountIdAndNormalizedName(accountId, normalizedName)) {
            throw ApiContractException.conflict("equipment_name_conflict", "当前账号已存在同名装备")
        }
    }

    private fun personalSummary(
        accountId: String,
        projections: List<PersonalEquipmentProjection> = ownedEquipment(accountId).map(::toEquipmentProjection)
    ): PersonalEquipmentSummary = PersonalEquipmentSummary(
        equipmentItemCount = projections.size,
        equipmentListCount = listOwnershipRepository.countByAccountId(accountId).toInt(),
        knownTotalWeight = WeightProjection(
            projections.sumOf { item -> (item.unitWeight?.grams ?: 0L) * item.ownedQuantity.toLong() }
        ),
        missingWeightItemCount = projections.count { it.unitWeight == null }
    )

    private fun toEquipmentProjection(record: PersonalEquipmentRecord) = PersonalEquipmentProjection(
        identity = record.id,
        name = record.name,
        ownedQuantity = record.ownedQuantity,
        unitWeight = record.unitWeightGrams?.let(::WeightProjection)
    )

    private fun toListDetail(list: UserEquipmentListRecord): EquipmentListDetailProjection {
        val members = memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc(list.id).map { member ->
            val equipment = equipmentRepository.findById(member.personalEquipmentId)
                .orElseThrow { ApiContractException.notFound() }
            EquipmentListMemberProjection(
                identity = equipment.id,
                name = equipment.name,
                ownedQuantity = equipment.ownedQuantity,
                unitWeight = equipment.unitWeightGrams?.let(::WeightProjection),
                quantity = member.quantity
            )
        }
        return EquipmentListDetailProjection(
            identity = list.id,
            name = list.name,
            members = members,
            summary = EquipmentListSummary(
                itemCount = members.size,
                knownTotalWeight = WeightProjection(
                    members.sumOf { item -> (item.unitWeight?.grams ?: 0L) * item.quantity.toLong() }
                ),
                missingWeightItemCount = members.count { it.unitWeight == null }
            )
        )
    }
}
