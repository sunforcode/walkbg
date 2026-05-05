package org.example.equipment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class EquipmentCreateRequest(
    @field:NotBlank(message = "装备名称不能为空")
    val name: String,
    
    val category: Int = 10,
    
    val weight: BigDecimal = BigDecimal.ZERO,
    
    @field:Positive(message = "数量必须大于0")
    val quantity: Int = 1,
    
    val weightUnit: Int = 0,
    
    val description: String? = null
)
