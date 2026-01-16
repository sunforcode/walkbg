package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto

/**
 * 营地DTO
 */
data class CampsiteDto(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?,
    @JsonProperty("campsite_type")
    val campsiteType: Int,
    val notes: String?,
    val creator: UserBasicDto?,
    @JsonProperty("created_at")
    val createdAt: Long, // 改为时间戳（秒）
    @JsonProperty("updated_at")
    val updatedAt: Long // 改为时间戳（秒）
) {
    companion object {
        /**
         * 从Campsite实体创建DTO
         */
        fun fromCampsite(campsite: org.example.route.model.Campsite): CampsiteDto {
            return CampsiteDto(
                id = campsite.id,
                name = campsite.name,
                description = campsite.description,
                latitude = campsite.latitude,
                longitude = campsite.longitude,
                elevation = campsite.elevation,
                campsiteType = campsite.campsiteType,
                notes = campsite.notes,
                creator = null,  // 需要通过 UserRepository 查询
                createdAt = campsite.createdAt.epochSecond, // 转换为时间戳
                updatedAt = campsite.updatedAt.epochSecond // 转换为时间戳
            )
        }
    }
}
