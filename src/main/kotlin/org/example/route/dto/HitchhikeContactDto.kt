package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 搭车联系人DTO
 */
data class HitchhikeContactDto(
    val id: String,
    val name: String,
    val phone: String,
    val description: String?,
    val location: String?,
    val price: Double?,
    @JsonProperty("last_verified")
    val lastVerified: Boolean
)
