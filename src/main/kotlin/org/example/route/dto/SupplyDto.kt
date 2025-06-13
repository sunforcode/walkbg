package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

/**
 * 补给点DTO
 */
data class SupplyDto(
    val id: String,
    val name: String,
    val description: String?,
    @JsonProperty("route_id")
    val routeId: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val elevation: BigDecimal?,
    @JsonProperty("supply_type")
    val supplyType: Int?, // 0: 商店, 1: 餐厅, 2: 自动售货机, 3: 紧急补给点, 4: 其他
    @JsonProperty("last_verified")
    val lastVerified: String?, // 最后验证者的用户ID
    @JsonProperty("last_verified_at")
    val lastVerifiedAt: Instant?, // 最后验证时间
    @JsonProperty("updated_by")
    val updatedBy: String?,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
)

data class WaypointDto(
    val id: String,
    val name: String,
    val description: String?,
    @JsonProperty("route_id")
    val routeId: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val elevation: BigDecimal?,
    @JsonProperty("supply_type")
    val supplyType: Int?, // 0: 商店, 1: 餐厅, 2: 自动售货机, 3: 紧急补给点, 4: 其他
    @JsonProperty("last_verified")
    val lastVerified: String?, // 最后验证者的用户ID
    @JsonProperty("last_verified_at")
    val lastVerifiedAt: Instant?, // 最后验证时间
    @JsonProperty("updated_by")
    val updatedBy: String?,
    @JsonProperty("is_active")
    val isActive: Boolean,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
)

/**
 * 补给点创建请求DTO
 */
data class SupplyCreateRequest(
    val id: String? = null,
    val name: String,
    val description: String?,
    @JsonProperty("route_id")
    val routeId: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val elevation: BigDecimal?,
    @JsonProperty("supply_type")
    val supplyType: Int?, // 0: 商店, 1: 餐厅, 2: 自动售货机, 3: 紧急补给点, 4: 其他
    @JsonProperty("last_verified")
    val lastVerified: String?, // 最后验证者的用户ID
    @JsonProperty("updated_by")
    val updatedBy: String?
)