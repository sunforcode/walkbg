package org.example.dto

import org.example.model.*
import java.time.Instant

// 装备清单列表响应DTO
data class EMEquipmentListResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val routeName: String?,
    val tripDays: Int,
    val personCount: Int,
    val seasons: List<String>,
    val totalWeight: Double,
    val totalItems: Int,
    val status: String,
    val isOfficial: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

// 装备清单详情响应DTO
data class EMEquipmentListDetailResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val routeId: String?,
    val routeName: String?,
    val tripId: String?,
    val tripDays: Int,
    val personCount: Int,
    val seasons: List<String>,
    val equipments: List<EMEquipmentItemResponse>,
    val totalWeight: Double,
    val baseWeight: Double,
    val consumableWeight: Double,
    val wornWeight: Double,
    val creatorId: String?,
    val creatorName: String?,
    val tags: List<String>,
    val isOfficial: Boolean,
    val isTemplate: Boolean,
    val templateId: String?,
    val status: String,
    val lastUsedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

// 装备项目响应DTO
data class EMEquipmentItemResponse(
    val id: String,
    val name: String,
    val category: String,
    val description: String?,
    val weight: Double,
    val weightUnit: String,
    val quantity: Int,
    val necessity: String,
    val prepared: Boolean,
    val isOwned: Boolean,
    val isShared: Boolean,
    val sharedPersonCount: Int?,
    val brand: String?,
    val model: String?,
    val price: Double?,
    val condition: String?,
    val imageUrl: String?,
    val notes: String?
)

// 创建装备清单请求DTO
data class EMCreateEquipmentListRequest(
    val name: String,
    val description: String?,
    val type: String,
    val routeId: String?,
    val routeName: String?,
    val tripId: String?,
    val tripDays: Int,
    val personCount: Int,
    val seasons: List<String>,
    val equipments: List<EMCreateEquipmentItemRequest>?,
    val tags: List<String>?
)

// 从模板创建装备清单请求DTO
data class EMCreateFromTemplateRequest(
    val templateId: String,
    val name: String,
    val description: String?,
    val routeId: String?,
    val routeName: String?,
    val tripId: String?,
    val tripDays: Int,
    val personCount: Int
)

// 更新装备清单请求DTO
data class EMUpdateEquipmentListRequest(
    val name: String?,
    val description: String?,
    val type: String?,
    val routeId: String?,
    val routeName: String?,
    val tripId: String?,
    val tripDays: Int?,
    val personCount: Int?,
    val seasons: List<String>?,
    val tags: List<String>?,
    val status: String?
)

// 创建装备项目请求DTO
data class EMCreateEquipmentItemRequest(
    val name: String,
    val category: String,
    val description: String?,
    val weight: Double,
    val weightUnit: String?,
    val quantity: Int?,
    val necessity: String?,
    val brand: String?,
    val model: String?,
    val price: Double?,
    val isOwned: Boolean?,
    val isShared: Boolean?,
    val sharedPersonCount: Int?,
    val condition: String?,
    val notes: String?
)

// 更新装备项目请求DTO
data class EMUpdateEquipmentItemRequest(
    val name: String?,
    val category: String?,
    val description: String?,
    val weight: Double?,
    val quantity: Int?,
    val necessity: String?,
    val prepared: Boolean?,
    val brand: String?,
    val model: String?,
    val price: Double?,
    val isOwned: Boolean?,
    val isShared: Boolean?,
    val sharedPersonCount: Int?,
    val condition: String?,
    val notes: String?
)

// 批量更新装备准备状态请求DTO
data class EMUpdatePreparationStatusRequest(
    val items: List<EMItemPreparationStatus>
)

data class EMItemPreparationStatus(
    val id: String,
    val prepared: Boolean
)

// 装备清单统计响应DTO
data class EMEquipmentListStatsResponse(
    val totalWeight: Double,
    val baseWeight: Double,
    val consumableWeight: Double,
    val wornWeight: Double,
    val weightPerPersonPerDay: Double,
    val totalItems: Int,
    val essentialItems: Int,
    val recommendedItems: Int,
    val optionalItems: Int,
    val preparedItems: Int,
    val preparationPercentage: Double,
    val categoryDistribution: List<EMCategoryDistribution>,
    val heaviestItems: List<EMHeavyItemInfo>
)

data class EMCategoryDistribution(
    val category: String,
    val count: Int,
    val weight: Double
)

data class EMHeavyItemInfo(
    val id: String,
    val name: String,
    val category: String,
    val weight: Double,
    val necessity: String
)

// 通用API响应包装类
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)