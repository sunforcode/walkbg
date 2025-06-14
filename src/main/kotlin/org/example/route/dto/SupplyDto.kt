package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto
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
    val updatedBy: UserBasicDto?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("updated_at")
    val updatedAt: Instant
) {
    companion object {
        /**
         * 从Supply实体创建DTO
         */
        fun fromSupply(supply: org.example.route.model.Supply): SupplyDto {
            return SupplyDto(
                id = supply.id,
                name = supply.name,
                description = supply.description,
                routeId = supply.route?.id,
                latitude = supply.latitude?.let { BigDecimal(it.toString()) },
                longitude = supply.longitude?.let { BigDecimal(it.toString()) },
                elevation = supply.elevation?.let { BigDecimal(it.toString()) },
                supplyType = supply.supplyType,
                lastVerified = supply.lastVerified,
                lastVerifiedAt = supply.lastVerifiedAt,
                updatedBy = supply.updatedByUser?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt
                    )
                },
                createdAt = supply.createdAt,
                updatedAt = supply.updatedAt
            )
        }
    }
}

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