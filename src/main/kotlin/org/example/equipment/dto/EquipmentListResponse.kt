package org.example.equipment.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

data class EquipmentListResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: Int,
    val typeName: String,
    @JsonProperty("trip_id")
    val tripId: String?,
    @JsonProperty("creator_id")
    val creatorId: String?,
    @JsonProperty("total_weight")
    val totalWeight: BigDecimal,
    @JsonProperty("person_count")
    val personCount: Int,
    val status: Int,
    val statusName: String,
    @JsonProperty("created_at")
    val createdAt: Long,
    @JsonProperty("updated_at")
    val updatedAt: Long,
    @JsonProperty("item_count")
    val itemCount: Long = 0
) {
    companion object {
        private val TYPE_NAMES = mapOf(
            0 to "个人装备",
            1 to "团队装备",
            2 to "模板装备"
        )

        private val STATUS_NAMES = mapOf(
            0 to "规划中",
            1 to "准备中",
            2 to "已完成",
            3 to "已归档"
        )

        fun fromEntity(
            list: org.example.equipment.model.EquipmentList,
            itemCount: Long = 0
        ): EquipmentListResponse {
            return EquipmentListResponse(
                id = list.id,
                name = list.name,
                description = null,
                type = list.type,
                typeName = TYPE_NAMES[list.type] ?: "其他",
                tripId = list.tripId,
                creatorId = list.creatorId,
                totalWeight = list.totalWeight,
                personCount = list.personCount,
                status = list.status,
                statusName = STATUS_NAMES[list.status] ?: "未知",
                createdAt = list.createdAt.epochSecond,
                updatedAt = list.updatedAt.epochSecond,
                itemCount = itemCount
            )
        }
    }
}
