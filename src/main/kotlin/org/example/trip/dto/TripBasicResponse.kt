package org.example.trip.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.trip.model.Trip

/**
 * 行程基础信息响应DTO
 */
data class TripBasicResponse(
    val id: String,
    val name: String,
    val description: String,
    @JsonProperty("start_date")
    val startDate: Long?, // 时间戳（秒）
    @JsonProperty("end_date")
    val endDate: Long?, // 时间戳（秒）
    val status: Int,
    @JsonProperty("route_ids")
    val routeIds: List<String>, // 路线ID列表
    @JsonProperty("primary_route_id")
    val primaryRouteId: String?,
    val participants: List<UserBasicInfo>, // 参与者列表
    @JsonProperty("participant_count")
    val participantCount: Int, // 参与者数量
    @JsonProperty("organizer_id")
    val organizerId: String,
    @JsonProperty("equipment_list_id")
    val equipmentListId: String?,
    @JsonProperty("meal_plan_id")
    val mealPlanId: String?,
    @JsonProperty("water_plan_id")
    val waterPlanId: String?,
    val itinerary: List<DailyPlanInfo>, // 行程安排
    @JsonProperty("cover_url")
    val coverUrl: String?,
    @JsonProperty("image_urls")
    val imageUrls: List<String>?,
    val budget: Double?,
    @JsonProperty("actual_cost")
    val actualCost: Double?,
    val notes: String?,
    @JsonProperty("privacy_setting")
    val privacySetting: String, // 转换为字符串: "public", "friends", "private"
    @JsonProperty("created_at")
    val createdAt: Long, // 时间戳（秒）
    @JsonProperty("updated_at")
    val updatedAt: Long // 时间戳（秒）
) {
    companion object {
        /**
         * 从Trip实体创建基础响应DTO
         */
        fun fromTrip(trip: Trip): TripBasicResponse {
            return TripBasicResponse(
                id = trip.id,
                name = trip.name,
                description = trip.description ?: "",
                startDate = trip.startDate?.epochSecond,
                endDate = trip.endDate?.epochSecond,
                status = trip.status,
                routeIds = if (trip.primaryRouteId != null) listOf(trip.primaryRouteId!!) else emptyList(),
                primaryRouteId = trip.primaryRouteId,
                participants = emptyList(), // 需要通过Repository查询
                participantCount = 0, // 需要通过Repository查询
                organizerId = trip.organizerId,
                equipmentListId = null,
                mealPlanId = null,
                waterPlanId = null,
                itinerary = emptyList(), // 需要通过Repository查询
                coverUrl = trip.coverUrl,
                imageUrls = emptyList(),
                budget = trip.budget?.toDouble(),
                actualCost = trip.actualCost?.toDouble(),
                notes = trip.notes,
                privacySetting = when (trip.privacySetting) {
                    0 -> "public"
                    1 -> "friends"
                    2 -> "private"
                    else -> "public"
                },
                createdAt = trip.createdAt.epochSecond,
                updatedAt = trip.updatedAt.epochSecond
            )
        }
    }
}

/**
 * 用户基础信息（用于参与者列表）
 */
data class UserBasicInfo(
    val id: String,
    val username: String,
    val nickname: String?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?
)

/**
 * 每日行程基础信息
 */
data class DailyPlanInfo(
    val day: Int,
    val date: Long?, // 时间戳（秒）
    val title: String,
    val description: String?
)
