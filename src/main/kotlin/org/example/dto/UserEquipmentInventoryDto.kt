package org.example.dto

import java.time.Instant

// 用户装备库响应DTO
data class EMUserEquipmentInventoryResponse(
    val userId: String,
    val lastUpdatedAt: Instant,
    val equipments: List<EMUserEquipmentItemResponse>,
    val statistics: EMInventoryStatistics
)

// 用户装备项目响应DTO
data class EMUserEquipmentItemResponse(
    val id: String,
    val name: String,
    val category: String,
    val weight: Double,
    val brand: String?,
    val model: String?,
    val condition: String?,
    val purchaseDate: Instant?,
    val usageCount: Int
)

// 装备库统计信息DTO
data class EMInventoryStatistics(
    val totalItems: Int,
    val totalValue: Double,
    val categoryDistribution: List<EMCategoryCount>,
    val conditionDistribution: List<EMConditionCount>
)

data class EMCategoryCount(
    val category: String,
    val count: Int,
    val value: Double
)

data class EMConditionCount(
    val condition: String,
    val count: Int
)

// 添加装备到用户装备库请求DTO
data class EMAddEquipmentToInventoryRequest(
    val name: String,
    val category: String,
    val description: String?,
    val weight: Double,
    val weightUnit: String?,
    val brand: String?,
    val model: String?,
    val price: Double?,
    val purchaseDate: Instant?,
    val purchaseLink: String?,
    val condition: String?,
    val usageCount: Int?,
    val imageUrl: String?,
    val notes: String?
)