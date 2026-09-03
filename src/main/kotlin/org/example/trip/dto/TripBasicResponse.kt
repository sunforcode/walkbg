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
         *
         * [equipmentListId] 由调用方（Controller）解析后传入，默认为 null。
         * 列表类接口（如 `GET /trips`）保持默认 null 以避免逐条查询装备清单造成的
         * N+1 问题；单个行程详情类接口应通过
         * [org.example.equipment.repository.EquipmentListRepository.findByTripId]
         * 解析出真实值后传入。
         *
         * [routeIds] 由调用方从行程-路线关联表查询后传入，是行程所含路线的权威来源。
         * 传入 null 或空列表时，会回退为由 [Trip.primaryRouteId] 推导单元素集合——
         * 该回退**仅用于兼容改造前创建的、没有任何关联记录的历史行程**，
         * 不得作为常规读取路径。列表场景应通过
         * [org.example.trip.service.TripService.getRouteIdsByTripIds] 批量取回后传入，
         * 避免逐条查询造成的 N+1。
         */
        fun fromTrip(
            trip: Trip,
            equipmentListId: String? = null,
            routeIds: List<String>? = null
        ): TripBasicResponse {
            return TripBasicResponse(
                id = trip.id,
                name = trip.name,
                description = trip.description ?: "",
                startDate = trip.startDate?.epochSecond,
                endDate = trip.endDate?.epochSecond,
                status = trip.status,
                routeIds = resolveRouteIds(trip, routeIds),
                primaryRouteId = trip.primaryRouteId,
                participants = emptyList(), // 需要通过Repository查询
                participantCount = 0, // 需要通过Repository查询
                organizerId = trip.organizerId,
                equipmentListId = equipmentListId,
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

        /**
         * 解析响应中的路线集合。
         *
         * 关联记录存在时直接采用，此时结果不受 [Trip.primaryRouteId] 影响。
         *
         * 仅当行程不存在任何关联记录时（即改造前创建的历史行程），才回退为由
         * [Trip.primaryRouteId] 推导的单元素集合。新建行程必定写入关联记录，
         * 因此不会走到回退分支。
         */
        private fun resolveRouteIds(trip: Trip, routeIds: List<String>?): List<String> {
            if (!routeIds.isNullOrEmpty()) {
                return routeIds
            }
            return trip.primaryRouteId?.let { listOf(it) } ?: emptyList()
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
