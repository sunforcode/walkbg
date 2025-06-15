package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto
import java.time.Instant

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
    @JsonProperty("verified_by")
    val verifiedBy: UserBasicDto?,
    val creator: UserBasicDto?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
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
                createdAt = campsite.createdAt,
                updatedAt = campsite.updatedAt,
                verifiedBy = campsite.verifiedBy?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt
                    )
                },
                creator = campsite.creator?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt
                    )
                }
            )
        }
    }
}
