package org.example.water.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto
import java.math.BigDecimal
import java.time.Instant

/**
 * 水源DTO
 */
data class WaterSourceDto(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?,
    @JsonProperty("water_type")
    val waterType: Int,
    @JsonProperty("water_quality")
    val waterQuality: Int,
    @JsonProperty("requires_treatment")
    val requiresTreatment: Boolean,
    val reliability: Double?,
    val notes: String?,
    @JsonProperty("last_verified")
    val lastVerified: String?,
    @JsonProperty("verified_by")
    val verifiedBy: UserBasicDto?,
    val creator: UserBasicDto?,
    @JsonProperty("created_at")
    val createdAt: Long, // 改为时间戳（秒）
    @JsonProperty("updated_at")
    val updatedAt: Long // 改为时间戳（秒）
) {
    companion object {
        /**
         * 从WaterSource实体创建DTO
         */
        fun fromWaterSource(water: org.example.water.model.WaterSource): WaterSourceDto {
            return WaterSourceDto(
                id = water.id,
                name = water.name,
                description = water.description,
                latitude = water.latitude,
                longitude = water.longitude,
                elevation = water.elevation,
                waterType = water.waterType,
                waterQuality = water.waterQuality,
                requiresTreatment = water.requiresTreatment,
                reliability = water.reliability,
                notes = water.notes,
                lastVerified = water.lastVerified,
                verifiedBy = water.verifiedBy?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt.epochSecond // 转换为时间戳
                    )
                },
                creator = water.creator?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt.epochSecond // 转换为时间戳
                    )
                },
                createdAt = water.createdAt.epochSecond, // 转换为时间戳
                updatedAt = water.updatedAt.epochSecond // 转换为时间戳
            )
        }
    }
}

/**
 * 水源创建请求DTO
 */
data class WaterSourceCreateRequest(
    val id: String? = null,
    val name: String,
    val description: String?,
    @JsonProperty("route_id")
    val routeId: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val elevation: BigDecimal?,
    @JsonProperty("water_type")
    val waterType: Int?, // 0: 泉水, 1: 河流, 2: 湖泊, 3: 井水, 4: 自来水, 5: 其他
    @JsonProperty("water_quality")
    val waterQuality: Int?,
    val reliability: Double?,
    @JsonProperty("requires_treatment")
    val requiresTreatment: Boolean = true,
    val notes: String?,
    @JsonProperty("verified_by")
    val verifiedBy: String?
)
