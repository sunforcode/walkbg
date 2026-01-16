package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto
import java.math.BigDecimal

/**
 * 补给点DTO
 */
data class SupplyDto(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val elevation: BigDecimal?,
    @JsonProperty("supply_type")
    val supplyType: Int?,
    @JsonProperty("last_verified")
    val lastVerified: String?,
    @JsonProperty("last_verified_at")
    val lastVerifiedAt: Long?, // 改为时间戳（秒）
    @JsonProperty("updated_by")
    val updatedBy: String?,
    @JsonProperty("updated_by_user")
    val updatedByUser: UserBasicDto?,
    val creator: UserBasicDto?,
    @JsonProperty("created_at")
    val createdAt: Long, // 改为时间戳（秒）
    @JsonProperty("updated_at")
    val updatedAt: Long // 改为时间戳（秒）
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
                latitude = supply.latitude, // 保持BigDecimal
                longitude = supply.longitude, // 保持BigDecimal
                elevation = supply.elevation, // 保持BigDecimal
                supplyType = supply.supplyType,
                lastVerified = supply.lastVerified,
                lastVerifiedAt = supply.lastVerifiedAt?.epochSecond, // 转换为时间戳
                updatedBy = supply.updatedBy,
                updatedByUser = null,  // 需要通过 UserRepository 查询
                creator = null,  // 需要通过 UserRepository 查询
                createdAt = supply.createdAt.epochSecond, // 转换为时间戳
                updatedAt = supply.updatedAt.epochSecond // 转换为时间戳
            )
        }
    }
}

/**
 * 补给点创建请求DTO
 */
data class SupplyCreateRequest(
    val id: String? = null,
    val name: String,
    val description: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val elevation: BigDecimal?,
    @JsonProperty("supply_type")
    val supplyType: Int?,
    @JsonProperty("last_verified")
    val lastVerified: String?,
    @JsonProperty("updated_by")
    val updatedBy: String?
)