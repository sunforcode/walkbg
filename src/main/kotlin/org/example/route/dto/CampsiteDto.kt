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
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
)
