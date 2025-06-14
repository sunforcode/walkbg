package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

/**
 * 联系人DTO
 */
data class ContactDto(
    val id: String,
    val name: String,
    val phone: String,
    val description: String?,
    val location: String?,
    val price: BigDecimal?,
    @JsonProperty("is_verified")
    val isVerified: Boolean,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
)
