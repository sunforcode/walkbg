package org.example.trip.personal.service

import org.example.common.contract.ApiContractException
import org.example.equipment.catalog.model.CatalogType
import org.example.equipment.dto.WeightProjection
import org.example.trip.personal.dto.*
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord

/** Pure validation and derivation shared by editing, generation and migration. */
object TripPreparationRules {
    val supplyKinds = setOf("food", "drinking_water", "fuel")

    fun validate(item: PersonalTripEquipmentItemRecord) {
        val type = item.equipmentType?.let { CatalogType.fromCode(it) ?: invalid("装备类型无效") }
        if (item.category != null && item.category !in CatalogType.categories) invalid("装备分类无效")
        if (type != null && item.category !in type.allowedCategories) invalid("分类与类型不匹配")
        if (item.unitLabel != null && item.unitLabel !in CatalogType.unitLabels) invalid("计数单位无效")
        if (item.variant?.isBlank() == true || item.weightScope?.isBlank() == true) invalid("配置说明不能为空")
        if (item.quantity <= 0 || item.unitWeightGrams?.let { !it.isFinite() || it <= 0 } == true) invalid("数量或重量无效")
        val allocation = listOf(item.backpackQuantity, item.wornQuantity, item.handheldQuantity)
        if (allocation.any { it != null } &&
            (allocation.any { it == null || it < 0 } || allocation.sumOf { it?.toLong() ?: 0 } != item.quantity.toLong())) {
            invalid("携带分配必须完整且合计等于数量")
        }
        if (item.packedQuantity?.let { it < 0 || it > (item.backpackQuantity ?: 0) } == true) invalid("装包数量超过背负数量")
        val supplyValues = listOf(item.supplyKind, item.supplyAmount, item.supplyAmountUnit, item.supplyContainerMode)
        if (supplyValues.any { it != null }) {
            if (supplyValues.any { it == null } || item.supplyKind !in supplyKinds ||
                item.category != item.supplyKind || item.equipmentType != item.supplyKind ||
                item.quantity != 1 || item.unitLabel != "份" ||
                item.supplyAmount?.let { !it.isFinite() || it <= 0 } != false ||
                item.supplyContainerMode !in setOf("included", "excluded")) invalid("补给配置不完整或无效")
            val allowedUnits = when (item.supplyKind) {
                "food" -> setOf("gram", "portion")
                "drinking_water" -> setOf("milliliter")
                else -> setOf("gram")
            }
            if (item.supplyAmountUnit !in allowedUnits) invalid("补给计量单位不匹配")
            if (item.unitWeightGrams != null && item.weightScope.isNullOrBlank()) invalid("补给已知重量必须说明范围")
            if (item.backpackQuantity != null &&
                (item.backpackQuantity != 1 || item.wornQuantity != 0 || item.handheldQuantity != 0)) invalid("补给只能背负或未分配")
        } else if (item.category in supplyKinds || item.equipmentType in supplyKinds) {
            invalid("补给分类必须填写本次实际补给")
        }
    }

    fun configuration(item: PersonalTripEquipmentItemRecord) = TripItemConfiguration(
        item.category, item.equipmentType, item.variant, item.unitLabel, item.weightScope,
        item.backpackQuantity?.let { TripCarryAllocation(it, requireNotNull(item.wornQuantity), requireNotNull(item.handheldQuantity)) },
        item.supplyKind?.let {
            TripSupply(it, TripSupplyAmount(requireNotNull(item.supplyAmount), requireNotNull(item.supplyAmountUnit)), requireNotNull(item.supplyContainerMode))
        }
    )

    fun apply(item: PersonalTripEquipmentItemRecord, config: TripItemConfiguration) {
        item.category = config.category
        item.equipmentType = config.equipmentType
        item.variant = config.variant?.trim()
        item.unitLabel = config.unitLabel
        item.weightScope = config.weightScope?.trim()
        item.backpackQuantity = config.carryAllocation?.backpackQuantity
        item.wornQuantity = config.carryAllocation?.wornQuantity
        item.handheldQuantity = config.carryAllocation?.handheldQuantity
        item.supplyKind = config.supply?.kind
        item.supplyAmount = config.supply?.amount?.value
        item.supplyAmountUnit = config.supply?.amount?.unit
        item.supplyContainerMode = config.supply?.containerMode
    }

    fun totalWeight(items: List<PersonalTripEquipmentItemRecord>): Double {
        val grams = items.sumOf { (it.unitWeightGrams ?: 0.0) * it.quantity }
        if (!grams.isFinite()) invalid("重量汇总超出可表示范围")
        return grams
    }

    fun preparation(tripId: String, revision: String, items: List<PersonalTripEquipmentItemRecord>): TripPreparationProjection {
        fun bucket(quantity: (PersonalTripEquipmentItemRecord) -> Int): EquipmentWeightBucket {
            val weight = items.sumOf { (it.unitWeightGrams ?: 0.0) * quantity(it) }
            if (!weight.isFinite()) invalid("重量汇总超出可表示范围")
            return EquipmentWeightBucket(WeightProjection(weight), items.count { quantity(it) > 0 && it.unitWeightGrams == null })
        }
        fun backpackCategory(category: String?) = bucket { if (it.category == category) it.backpackQuantity ?: 0 else 0 }
        val backpack = items.sumOf { (it.backpackQuantity ?: 0).toLong() }
        val packed = items.sumOf { (it.packedQuantity ?: 0).toLong() }
        return TripPreparationProjection(
            tripId, revision,
            TripWeightBreakdown(
                bucket { it.backpackQuantity ?: 0 }, bucket { it.wornQuantity ?: 0 },
                bucket { it.handheldQuantity ?: 0 }, bucket { if (it.backpackQuantity == null) it.quantity else 0 }
            ),
            TripBackpackBreakdown(
                bucket { if (it.category != null && it.category !in supplyKinds) it.backpackQuantity ?: 0 else 0 },
                backpackCategory("food"), backpackCategory("drinking_water"), backpackCategory("fuel"), backpackCategory(null)
            ),
            TripPackingProgress(backpack, packed, backpack - packed, items.count { it.backpackQuantity == null })
        )
    }

    private fun invalid(message: String): Nothing = throw ApiContractException.unprocessable("validation_failed", message)
}
