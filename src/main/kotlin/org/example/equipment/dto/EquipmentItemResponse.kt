package org.example.equipment.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

data class EquipmentItemResponse(
    val id: String,
    val name: String,
    val category: Int,
    val categoryName: String,
    val weight: BigDecimal,
    val weightUnit: Int,
    val weightUnitName: String,
    val quantity: Int,
    @JsonProperty("created_by")
    val createdBy: String?,
    @JsonProperty("created_at")
    val createdAt: Long,
    @JsonProperty("updated_at")
    val updatedAt: Long
) {
    companion object {
        private val CATEGORY_NAMES = mapOf(
            0 to "住宿装备",
            1 to "饮食装备",
            2 to "保暖装备",
            3 to "背包装备",
            4 to "导航装备",
            5 to "照明装备",
            6 to "急救装备",
            7 to "工具装备",
            8 to "电子装备",
            9 to "个人护理",
            10 to "其他装备"
        )

        private val WEIGHT_UNIT_NAMES = mapOf(
            0 to "克",
            1 to "千克",
            2 to "磅",
            3 to "盎司"
        )

        fun fromEntity(item: org.example.equipment.model.EquipmentItem): EquipmentItemResponse {
            return EquipmentItemResponse(
                id = item.id,
                name = item.name,
                category = item.category,
                categoryName = CATEGORY_NAMES[item.category] ?: "其他装备",
                weight = item.weight,
                weightUnit = item.weightUnit,
                weightUnitName = WEIGHT_UNIT_NAMES[item.weightUnit] ?: "克",
                quantity = item.quantity,
                createdBy = item.createdBy,
                createdAt = item.createdAt.epochSecond,
                updatedAt = item.updatedAt.epochSecond
            )
        }
    }
}
