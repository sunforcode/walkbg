package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.user.dto.UserBasicDto

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
    val lastVerified: Boolean,
    val creator: UserBasicDto?,
    @JsonProperty("created_at")
    val createdAt: Long, // 改为时间戳（秒）
    @JsonProperty("updated_at")
    val updatedAt: Long // 改为时间戳（秒）
) {
    companion object {
        /**
         * 从HitchhikeContact实体创建DTO
         */
        fun fromHitchhikeContact(contact: org.example.route.model.HitchhikeContact): HitchhikeContactDto {
            return HitchhikeContactDto(
                id = contact.id,
                name = contact.name,
                phone = contact.phone,
                description = contact.description,
                location = contact.location,
                price = contact.price,
                lastVerified = contact.lastVerified,
                creator = contact.creator?.let { user ->
                    UserBasicDto(
                        id = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                        createdAt = user.createdAt.epochSecond // 转换为时间戳
                    )
                },
                createdAt = contact.createdAt.epochSecond, // 转换为时间戳
                updatedAt = contact.updatedAt.epochSecond // 转换为时间戳
            )
        }
    }
}
