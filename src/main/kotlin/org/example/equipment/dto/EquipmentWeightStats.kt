package org.example.equipment.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.equipment.util.EquipmentWeightUtils
import java.math.BigDecimal
import java.math.RoundingMode

data class EquipmentWeightStats(
    
    @JsonProperty("total_weight")
    val totalWeight: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("base_weight")
    val baseWeight: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("consumable_weight")
    val consumableWeight: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("worn_weight")
    val wornWeight: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("weight_per_person")
    val weightPerPerson: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("weight_per_person_per_day")
    val weightPerPersonPerDay: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("person_count")
    val personCount: Int = 1,
    
    @JsonProperty("trip_days")
    val tripDays: Int = 1,
    
    @JsonProperty("total_items")
    val totalItems: Int = 0,
    
    @JsonProperty("formatted_total_weight")
    val formattedTotalWeight: String = "",
    
    @JsonProperty("formatted_base_weight")
    val formattedBaseWeight: String = "",
    
    @JsonProperty("formatted_weight_per_person")
    val formattedWeightPerPerson: String = ""
) {
    companion object {
        
        fun create(
            totalWeight: BigDecimal,
            baseWeight: BigDecimal = BigDecimal.ZERO,
            consumableWeight: BigDecimal = BigDecimal.ZERO,
            wornWeight: BigDecimal = BigDecimal.ZERO,
            personCount: Int = 1,
            tripDays: Int = 1,
            totalItems: Int = 0
        ): EquipmentWeightStats {
            val weightPerPerson = if (personCount > 0) {
                totalWeight.divide(BigDecimal(personCount), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            
            val totalPersonDays = personCount * tripDays
            val weightPerPersonPerDay = if (totalPersonDays > 0) {
                totalWeight.divide(BigDecimal(totalPersonDays), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            
            return EquipmentWeightStats(
                totalWeight = totalWeight,
                baseWeight = baseWeight,
                consumableWeight = consumableWeight,
                wornWeight = wornWeight,
                weightPerPerson = weightPerPerson,
                weightPerPersonPerDay = weightPerPersonPerDay,
                personCount = personCount,
                tripDays = tripDays,
                totalItems = totalItems,
                formattedTotalWeight = EquipmentWeightUtils.formatWeight(totalWeight),
                formattedBaseWeight = EquipmentWeightUtils.formatWeight(baseWeight),
                formattedWeightPerPerson = EquipmentWeightUtils.formatWeight(weightPerPerson)
            )
        }
        
        fun fromTotalWeight(
            totalWeight: BigDecimal,
            personCount: Int = 1,
            tripDays: Int = 1,
            totalItems: Int = 0
        ): EquipmentWeightStats {
            return create(
                totalWeight = totalWeight,
                personCount = personCount,
                tripDays = tripDays,
                totalItems = totalItems
            )
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "total_weight" to totalWeight,
            "base_weight" to baseWeight,
            "consumable_weight" to consumableWeight,
            "worn_weight" to wornWeight,
            "weight_per_person" to weightPerPerson,
            "weight_per_person_per_day" to weightPerPersonPerDay,
            "person_count" to personCount,
            "trip_days" to tripDays,
            "total_items" to totalItems,
            "formatted_total_weight" to formattedTotalWeight,
            "formatted_base_weight" to formattedBaseWeight,
            "formatted_weight_per_person" to formattedWeightPerPerson
        )
    }
}

data class EquipmentListFullStats(
    
    @JsonProperty("list_id")
    val listId: String,
    
    @JsonProperty("list_name")
    val listName: String,
    
    @JsonProperty("weight_stats")
    val weightStats: EquipmentWeightStats,
    
    @JsonProperty("total_items")
    val totalItems: Int = 0,
    
    @JsonProperty("essential_items")
    val essentialItems: Int = 0,
    
    @JsonProperty("recommended_items")
    val recommendedItems: Int = 0,
    
    @JsonProperty("optional_items")
    val optionalItems: Int = 0,
    
    @JsonProperty("prepared_items")
    val preparedItems: Int = 0,
    
    @JsonProperty("preparation_percentage")
    val preparationPercentage: Double = 0.0,
    
    @JsonProperty("total_value")
    val totalValue: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("owned_items")
    val ownedItems: Int = 0,
    
    @JsonProperty("items_to_buy")
    val itemsToBuy: Int = 0,
    
    @JsonProperty("value_to_buy")
    val valueToBuy: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("category_stats")
    val categoryStats: List<CategoryWeightStats> = emptyList()
)

data class CategoryWeightStats(
    
    @JsonProperty("category_code")
    val categoryCode: Int,
    
    @JsonProperty("category_name")
    val categoryName: String,
    
    @JsonProperty("item_count")
    val itemCount: Int = 0,
    
    @JsonProperty("total_weight")
    val totalWeight: BigDecimal = BigDecimal.ZERO,
    
    @JsonProperty("formatted_weight")
    val formattedWeight: String = "",
    
    @JsonProperty("weight_percentage")
    val weightPercentage: Double = 0.0
) {
    companion object {
        private val CATEGORY_NAMES = mapOf(
            0 to "住宿装备",
            1 to "饮食装备",
            2 to "保暖装备",
            3 to "背包装备",
            4 to "导航装备",
            5 to "照明装备",
            6 to "急救装备",
            7 to "工具装备",
            8 to "电子装备",
            9 to "个人护理",
            10 to "其他装备"
        )
        
        fun getCategoryName(categoryCode: Int): String {
            return CATEGORY_NAMES[categoryCode] ?: "未知分类"
        }
        
        fun create(
            categoryCode: Int,
            itemCount: Int = 0,
            totalWeight: BigDecimal = BigDecimal.ZERO,
            totalOverallWeight: BigDecimal = BigDecimal.ZERO
        ): CategoryWeightStats {
            val weightPercentage = if (totalOverallWeight > BigDecimal.ZERO) {
                totalWeight.divide(totalOverallWeight, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
                    .toDouble()
            } else {
                0.0
            }
            
            return CategoryWeightStats(
                categoryCode = categoryCode,
                categoryName = getCategoryName(categoryCode),
                itemCount = itemCount,
                totalWeight = totalWeight,
                formattedWeight = EquipmentWeightUtils.formatWeight(totalWeight),
                weightPercentage = weightPercentage
            )
        }
    }
}
