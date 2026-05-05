package org.example.equipment.dto

import jakarta.validation.constraints.NotBlank

data class EquipmentListCreateRequest(
    @field:NotBlank(message = "清单名称不能为空")
    val name: String,
    
    val description: String? = null,
    
    val type: Int = 0,
    
    val personCount: Int = 1,
    
    val tripId: String? = null,
    
    val templateId: String? = null
)
