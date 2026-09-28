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
import org.example.equipment.catalog.service.EquipmentCatalogApplicationService
import org.example.equipment.support.EquipmentCommandStore
import org.example.equipment.util.normalizeEquipmentSearchText
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class PersonalEquipmentFilter(
    val keyword: String? = null,
    val category: String? = null,
    val equipmentType: String? = null,
    val missingWeight: Boolean? = null
)

data class CreatePersonalEquipmentCommand(
    val name: String?,
    val ownedQuantity: Int?,
    val unitWeightGrams: Double?,
    val category: String? = null,
    val equipmentType: String? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null
)

data class UpdatePersonalEquipmentCommand(
    val name: String? = null,
    val ownedQuantity: Int? = null,
    val unitWeightGrams: Double? = null,
    val clearUnitWeight: Boolean = false,
    val hasName: Boolean = name != null,
    val hasOwnedQuantity: Boolean = ownedQuantity != null,
    val hasUnitWeight: Boolean = unitWeightGrams != null || clearUnitWeight,
    val category: String? = null,
    val equipmentType: String? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null,
    val hasCategory: Boolean = category != null,
    val hasEquipmentType: Boolean = equipmentType != null,
    val hasVariant: Boolean = variant != null,
    val hasUnitLabel: Boolean = unitLabel != null,
    val hasWeightScope: Boolean = weightScope != null
)

data class AdoptCatalogEquipmentCommand(
    val catalogEquipmentId: String,
    val ownedQuantity: Int,
    val name: String? = null,
    val unitWeightGrams: Double? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null,
    val hasUnitWeight: Boolean = unitWeightGrams != null,
    val hasVariant: Boolean = variant != null,
    val hasUnitLabel: Boolean = unitLabel != null,
    val hasWeightScope: Boolean = weightScope != null
)

@Service
class PersonalEquipmentApplicationService(
    private val equipmentRepository: PersonalEquipmentRecordRepository,
    private val equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository,
    private val listRepository: UserEquipmentListRecordRepository,
    private val listOwnershipRepository: EquipmentListOwnershipRepository,
    private val memberRepository: EquipmentListMemberRepository,
    private val domainService: PersonalEquipmentDomainService,
    private val commandStore: EquipmentCommandStore,
    private val catalogService: EquipmentCatalogApplicationService
) {
    @Transactional(readOnly = true)
    fun getEquipmentCollection(accountId: String, filter: PersonalEquipmentFilter = PersonalEquipmentFilter()): PersonalEquipmentCollectionResponse {
        domainService.validateFilter(filter)
        val all = ownedEquipment(accountId).map(::toEquipmentProjection)
        val keyword = filter.keyword?.let(::normalizeEquipmentSearchText)
        val items = all.filter {
            (keyword == null || normalizeEquipmentSearchText(it.name).contains(keyword)) &&
                (filter.category == null || it.category == filter.category) &&
                (filter.equipmentType == null || it.equipmentType == filter.equipmentType) &&
                (filter.missingWeight == null || (it.unitWeight == null) == filter.missingWeight)
        }
        return PersonalEquipmentCollectionResponse(items, personalSummary(accountId, all, writing = false))
    }

    @Transactional
    fun createEquipment(accountId: String, command: CreatePersonalEquipmentCommand): PersonalEquipmentMutationResponse {
        commandStore.lockAccount(accountId)
        val normalized = domainService.normalizeEquipmentName(command.name)
        val record = PersonalEquipmentRecord(
            IdGenerator.generateIdWithPrefix("pe"), normalized.display,
            domainService.requirePositiveQuantity(command.ownedQuantity, "拥有数量"),
            command.unitWeightGrams?.let(domainService::requirePositiveWeight),
            category = command.category, equipmentType = command.equipmentType,
            variant = domainService.optionalText(command.variant), unitLabel = command.unitLabel,
            weightScope = domainService.optionalText(command.weightScope),
            unitWeightSource = if (command.unitWeightGrams == null) null else "user_recorded"
        )
        return saveNewEquipment(accountId, normalized.comparison, record)
    }

    @Transactional
    fun adoptCatalogEquipment(accountId: String, idempotencyKey: String, command: AdoptCatalogEquipmentCommand): PersonalEquipmentMutationResponse =
        commandStore.execute(accountId, "personal-equipment:from-catalog", idempotencyKey, command, PersonalEquipmentMutationResponse::class.java) {
            val source = catalogService.getRecord(command.catalogEquipmentId)
            val spec = source.specifications
            val normalized = domainService.normalizeEquipmentName(command.name ?: source.name)
            val weight = if (command.hasUnitWeight) command.unitWeightGrams?.let(domainService::requirePositiveWeight) else spec?.unitWeightGrams
            val variant = if (command.hasVariant) domainService.optionalText(command.variant) else spec?.variant
            val unitLabel = if (command.hasUnitLabel) command.unitLabel else spec?.unitLabel
            val scope = if (command.hasWeightScope) domainService.optionalText(command.weightScope) else spec?.weightScope
            if (!command.hasUnitWeight && weight != null && (variant != spec?.variant || unitLabel != spec?.unitLabel || scope != spec?.weightScope)) {
                throw ApiContractException.unprocessable("validation_failed", "修改参考配置时必须明确新重量或清除重量")
            }
            val record = PersonalEquipmentRecord(
                IdGenerator.generateIdWithPrefix("pe"), normalized.display,
                domainService.requirePositiveQuantity(command.ownedQuantity, "拥有数量"), weight,
                category = source.category, equipmentType = source.equipmentType,
                variant = variant, unitLabel = unitLabel, weightScope = scope,
                unitWeightSource = if (weight == null) null else if (command.hasUnitWeight) "user_recorded" else "catalog_reference",
                catalogEquipmentId = source.identity
            )
            saveNewEquipment(accountId, normalized.comparison, record)
        }

    private fun saveNewEquipment(accountId: String, normalizedName: String, record: PersonalEquipmentRecord): PersonalEquipmentMutationResponse {
        domainService.validateConfiguration(record)
        ensureUniqueEquipmentName(accountId, normalizedName)
        equipmentRepository.save(record)
        equipmentOwnershipRepository.save(PersonalEquipmentOwnership(record.id, accountId, normalizedName))
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
        commandStore.lockAccount(accountId)
        val previous = requireOwnedEquipment(accountId, personalEquipmentId)
        val ownership = requireEquipmentOwnership(accountId, personalEquipmentId)
        val normalized = if (command.hasName) domainService.normalizeEquipmentName(command.name) else null
        if (normalized != null && equipmentOwnershipRepository.existsByAccountIdAndNormalizedNameAndPersonalEquipmentIdNot(
                accountId, normalized.comparison, personalEquipmentId)) {
            throw ApiContractException.conflict("equipment_name_conflict", "当前账号已存在同名装备")
        }
        val weight = if (!command.hasUnitWeight) previous.unitWeightGrams else if (command.clearUnitWeight) null else domainService.requirePositiveWeight(command.unitWeightGrams)
        val next = previous.copy(
            name = normalized?.display ?: previous.name,
            ownedQuantity = if (command.hasOwnedQuantity) domainService.requirePositiveQuantity(command.ownedQuantity, "拥有数量") else previous.ownedQuantity,
            unitWeightGrams = weight,
            unitWeightSource = if (!command.hasUnitWeight) previous.unitWeightSource else if (weight == null) null else "user_recorded",
            category = if (command.hasCategory) command.category else previous.category,
            equipmentType = if (command.hasEquipmentType) command.equipmentType else previous.equipmentType,
            variant = if (command.hasVariant) domainService.optionalText(command.variant) else previous.variant,
            unitLabel = if (command.hasUnitLabel) command.unitLabel else previous.unitLabel,
            weightScope = if (command.hasWeightScope) domainService.optionalText(command.weightScope) else previous.weightScope,
            updatedAt = Instant.now()
        )
        domainService.validateConfiguration(next)
        if (!command.hasUnitWeight && (next.unitLabel != previous.unitLabel ||
                (previous.unitWeightSource == "catalog_reference" && (next.category != previous.category ||
                    next.equipmentType != previous.equipmentType || next.variant != previous.variant || next.weightScope != previous.weightScope)))) {
            throw ApiContractException.unprocessable("validation_failed", "修改计量单位或参考配置时必须明确新重量或清除重量")
        }
        equipmentRepository.save(next)
        if (normalized != null) equipmentOwnershipRepository.save(ownership.copy(normalizedName = normalized.comparison))
        return PersonalEquipmentMutationResponse(toEquipmentProjection(next), personalSummary(accountId))
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
        commandStore.lockAccount(accountId)
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
            val detail = toListDetail(list, writing = false)
            EquipmentListSummaryProjection(detail.identity, detail.name, detail.summary)
        }
    )

    @Transactional
    fun createEquipmentList(accountId: String, name: String?): EquipmentListCreateResponse {
        commandStore.lockAccount(accountId)
        val normalizedName = domainService.normalizeListName(name)
        val id = IdGenerator.generateIdWithPrefix("el")
        val list = listRepository.save(UserEquipmentListRecord(id, normalizedName))
        listOwnershipRepository.save(EquipmentListOwnership(id, accountId))
        return EquipmentListCreateResponse(toListDetail(list), personalSummary(accountId))
    }

    @Transactional
    fun copyEquipmentList(accountId: String, equipmentListId: String, idempotencyKey: String, name: String): EquipmentListCreateResponse =
        commandStore.execute(accountId, "equipment-lists/$equipmentListId:copy", idempotencyKey, mapOf("name" to name), EquipmentListCreateResponse::class.java) {
            requireOwnedList(accountId, equipmentListId)
            val normalizedName = domainService.normalizeListName(name)
            val members = memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc(equipmentListId)
            members.forEach { requireOwnedEquipment(accountId, it.personalEquipmentId) }
            val id = IdGenerator.generateIdWithPrefix("el")
            val list = listRepository.save(UserEquipmentListRecord(id, normalizedName))
            listOwnershipRepository.save(EquipmentListOwnership(id, accountId))
            members.forEach { memberRepository.save(EquipmentListMember(id, it.personalEquipmentId, it.quantity)) }
            EquipmentListCreateResponse(toListDetail(list), personalSummary(accountId))
        }

    @Transactional(readOnly = true)
    fun getEquipmentList(accountId: String, equipmentListId: String): EquipmentListDetailProjection =
        toListDetail(requireOwnedList(accountId, equipmentListId), writing = false)

    @Transactional
    fun renameEquipmentList(
        accountId: String,
        equipmentListId: String,
        name: String?
    ): EquipmentListMutationResponse {
        commandStore.lockAccount(accountId)
        val list = requireOwnedList(accountId, equipmentListId)
        list.name = domainService.normalizeListName(name)
        list.updatedAt = Instant.now()
        return EquipmentListMutationResponse(toListDetail(listRepository.save(list)))
    }

    @Transactional
    fun deleteEquipmentList(accountId: String, equipmentListId: String): EquipmentListDeletionResponse {
        commandStore.lockAccount(accountId)
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
        commandStore.lockAccount(accountId)
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
        commandStore.lockAccount(accountId)
        val list = requireOwnedList(accountId, equipmentListId)
        requireOwnedEquipment(accountId, personalEquipmentId)
        val id = EquipmentListMemberId(equipmentListId, personalEquipmentId)
        if (!memberRepository.existsById(id)) throw ApiContractException.notFound()
        memberRepository.deleteById(id)
        return EquipmentListMutationResponse(toListDetail(list))
    }

    private fun ownedEquipment(accountId: String): List<PersonalEquipmentRecord> =
        equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(accountId)
            .mapNotNull { equipmentRepository.findById(it.personalEquipmentId).orElse(null) }.sortedBy { it.id }

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
        projections: List<PersonalEquipmentProjection> = ownedEquipment(accountId).map(::toEquipmentProjection),
        writing: Boolean = true
    ): PersonalEquipmentSummary = PersonalEquipmentSummary(
        equipmentItemCount = projections.size,
        equipmentListCount = listOwnershipRepository.countByAccountId(accountId).toInt(),
        knownTotalWeight = totalWeight(
            projections.sumOf { item -> (item.unitWeight?.grams ?: 0.0) * item.ownedQuantity }, writing
        ),
        missingWeightItemCount = projections.count { it.unitWeight == null }
    )

    private fun toEquipmentProjection(record: PersonalEquipmentRecord) = PersonalEquipmentProjection(
        identity = record.id,
        name = record.name,
        ownedQuantity = record.ownedQuantity,
        unitWeight = record.unitWeightGrams?.let(::WeightProjection),
        category = record.category, equipmentType = record.equipmentType, variant = record.variant,
        unitLabel = record.unitLabel, weightScope = record.weightScope,
        unitWeightSource = record.unitWeightGrams?.let { record.unitWeightSource }, catalogEquipmentId = record.catalogEquipmentId
    )

    private fun toListDetail(list: UserEquipmentListRecord, writing: Boolean = true): EquipmentListDetailProjection {
        val members = memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc(list.id).map { member ->
            val equipment = equipmentRepository.findById(member.personalEquipmentId)
                .orElseThrow { ApiContractException.notFound() }
            EquipmentListMemberProjection(
                identity = equipment.id,
                name = equipment.name,
                ownedQuantity = equipment.ownedQuantity,
                unitWeight = equipment.unitWeightGrams?.let(::WeightProjection),
                quantity = member.quantity,
                category = equipment.category, equipmentType = equipment.equipmentType, variant = equipment.variant,
                unitLabel = equipment.unitLabel, weightScope = equipment.weightScope,
                unitWeightSource = equipment.unitWeightGrams?.let { equipment.unitWeightSource }, catalogEquipmentId = equipment.catalogEquipmentId
            )
        }
        return EquipmentListDetailProjection(
            identity = list.id,
            name = list.name,
            members = members,
            summary = EquipmentListSummary(
                itemCount = members.size,
                knownTotalWeight = totalWeight(
                    members.sumOf { item -> (item.unitWeight?.grams ?: 0.0) * item.quantity }, writing
                ),
                missingWeightItemCount = members.count { it.unitWeight == null }
            )
        )
    }
    private fun totalWeight(grams: Double, writing: Boolean): WeightProjection {
        if (!grams.isFinite()) {
            if (writing) throw ApiContractException.unprocessable("validation_failed", "汇总重量超出支持范围")
            throw ApiContractException.serviceUnavailable("personal_equipment_unavailable", "个人装备服务暂不可用")
        }
        return WeightProjection(grams)
    }

}
