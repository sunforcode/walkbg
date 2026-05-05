package org.example.equipment.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class EquipmentTemplateResponse(
    val id: String,
    val name: String,
    val category: Int,
    val categoryName: String,
    val description: String?,
    val type: Int,
    val typeName: String,
    @JsonProperty("is_official")
    val isOfficial: Boolean,
    @JsonProperty("creator_id")
    val creatorId: String?,
    @JsonProperty("creator_name")
    val creatorName: String?,
    @JsonProperty("usage_count")
    val usageCount: Int,
    val rating: Double,
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

        private val TYPE_NAMES = mapOf(
            0 to "短途徒步",
            1 to "长途徒步",
            2 to "露营",
            3 to "登山",
            4 to "徒步旅行",
            5 to "自定义"
        )

        fun fromEntity(template: org.example.equipment.model.EquipmentTemplate): EquipmentTemplateResponse {
            return EquipmentTemplateResponse(
                id = template.id,
                name = template.name,
                category = template.category,
                categoryName = CATEGORY_NAMES[template.category] ?: "其他装备",
                description = template.description,
                type = template.type,
                typeName = TYPE_NAMES[template.type] ?: "自定义",
                isOfficial = template.isOfficial,
                creatorId = template.creatorId,
                creatorName = template.creatorName,
                usageCount = template.usageCount,
                rating = template.rating,
                createdAt = template.createdAt.epochSecond,
                updatedAt = template.updatedAt.epochSecond
            )
        }
    }
}
