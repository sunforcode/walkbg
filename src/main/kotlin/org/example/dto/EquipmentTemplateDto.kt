package org.example.dto

import java.time.Instant

// 装备模板列表响应DTO
data class EMEquipmentTemplateResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val seasons: List<String>,
    val isOfficial: Boolean,
    val usageCount: Int,
    val rating: Double,
    val createdAt: Instant
)

// 装备模板详情响应DTO
data class EMEquipmentTemplateDetailResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val seasons: List<String>,
    val equipments: List<EMTemplateEquipmentItemResponse>,
    val tags: List<String>,
    val isOfficial: Boolean,
    val creatorId: String?,
    val creatorName: String?,
    val usageCount: Int,
    val rating: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)

// 模板装备项目响应DTO
data class EMTemplateEquipmentItemResponse(
    val id: String,
    val name: String,
    val category: String,
    val description: String?,
    val weight: Double,
    val weightUnit: String,
    val quantity: Int,
    val necessity: String,
    val brand: String?,
    val model: String?,
    val isShared: Boolean,
    val sharedPersonCount: Int?,
    val imageUrl: String?,
    val notes: String?
)

// 创建装备模板请求DTO
data class EMCreateEquipmentTemplateRequest(
    val name: String,
    val description: String?,
    val type: String,
    val seasons: List<String>,
    val equipments: List<EMCreateTemplateEquipmentItemRequest>,
    val tags: List<String>?
)

// 从装备清单创建模板请求DTO
data class EMCreateTemplateFromListRequest(
    val listId: String,
    val name: String,
    val description: String?,
    val tags: List<String>?
)

// 创建模板装备项目请求DTO
data class EMCreateTemplateEquipmentItemRequest(
    val name: String,
    val category: String,
    val description: String?,
    val weight: Double,
    val weightUnit: String?,
    val quantity: Int?,
    val necessity: String?,
    val brand: String?,
    val model: String?,
    val isShared: Boolean?,
    val sharedPersonCount: Int?,
    val notes: String?
)