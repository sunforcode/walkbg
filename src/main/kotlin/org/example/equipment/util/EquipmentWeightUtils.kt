package org.example.equipment.util

import org.example.equipment.model.EquipmentItem
import java.math.BigDecimal
import java.math.RoundingMode

object EquipmentWeightUtils {
    
    const val GRAMS_PER_KILOGRAM = 1000
    
    enum class WeightUnit(val code: Int, val label: String, val toGramsFactor: BigDecimal) {
        GRAM(0, "克", BigDecimal.ONE),
        KILOGRAM(1, "千克", BigDecimal(GRAMS_PER_KILOGRAM)),
        POUND(2, "磅", BigDecimal("453.592")),
        OUNCE(3, "盎司", BigDecimal("28.3495"));
        
        companion object {
            fun fromCode(code: Int): WeightUnit {
                return values().find { it.code == code } ?: GRAM
            }
        }
    }
    
    enum class WeightCategory(val threshold: Int, val label: String) {
        ULTRA_LIGHT(100, "超轻量"),
        LIGHT(500, "轻量"),
        MEDIUM(1000, "中量"),
        HEAVY(Int.MAX_VALUE, "重量");
        
        companion object {
            fun fromGrams(grams: Int): WeightCategory {
                return when {
                    grams < ULTRA_LIGHT.threshold -> ULTRA_LIGHT
                    grams < LIGHT.threshold -> LIGHT
                    grams < MEDIUM.threshold -> MEDIUM
                    else -> HEAVY
                }
            }
        }
    }
    
    fun convertToGrams(weight: BigDecimal, unit: Int): BigDecimal {
        val weightUnit = WeightUnit.fromCode(unit)
        return weight.multiply(weightUnit.toGramsFactor)
    }
    
    fun convertFromGrams(grams: BigDecimal, targetUnit: Int): BigDecimal {
        val targetWeightUnit = WeightUnit.fromCode(targetUnit)
        return if (targetWeightUnit.toGramsFactor == BigDecimal.ZERO) {
            grams
        } else {
            grams.divide(targetWeightUnit.toGramsFactor, 4, RoundingMode.HALF_UP)
        }
    }
    
    fun calculateTotalWeight(items: List<EquipmentItem>): BigDecimal {
        return items.fold(BigDecimal.ZERO) { total, item ->
            val itemWeightInGrams = convertToGrams(item.weight, item.weightUnit)
            val quantity = BigDecimal(item.quantity)
            total.add(itemWeightInGrams.multiply(quantity))
        }
    }
    
    fun calculateTotalWeightWithQuantity(
        items: List<Pair<EquipmentItem, Int>>
    ): BigDecimal {
        return items.fold(BigDecimal.ZERO) { total, (item, qty) ->
            val itemWeightInGrams = convertToGrams(item.weight, item.weightUnit)
            val quantity = BigDecimal(qty)
            total.add(itemWeightInGrams.multiply(quantity))
        }
    }
    
    fun calculateWeightPerPerson(
        totalWeight: BigDecimal,
        personCount: Int
    ): BigDecimal {
        return if (personCount <= 0) {
            BigDecimal.ZERO
        } else {
            totalWeight.divide(BigDecimal(personCount), 2, RoundingMode.HALF_UP)
        }
    }
    
    fun calculateWeightPerPersonPerDay(
        totalWeight: BigDecimal,
        personCount: Int,
        tripDays: Int
    ): BigDecimal {
        val totalPersonDays = personCount * tripDays
        return if (totalPersonDays <= 0) {
            BigDecimal.ZERO
        } else {
            totalWeight.divide(BigDecimal(totalPersonDays), 2, RoundingMode.HALF_UP)
        }
    }
    
    fun formatWeight(grams: BigDecimal): String {
        return when {
            grams >= BigDecimal(GRAMS_PER_KILOGRAM) -> {
                val kilograms = grams.divide(BigDecimal(GRAMS_PER_KILOGRAM), 2, RoundingMode.HALF_UP)
                "$kilograms kg"
            }
            else -> "${grams.setScale(0, RoundingMode.HALF_UP)} g"
        }
    }
    
    fun formatWeightDetailed(grams: BigDecimal): String {
        return when {
            grams >= BigDecimal(GRAMS_PER_KILOGRAM) -> {
                val kilograms = grams.divide(BigDecimal(GRAMS_PER_KILOGRAM), 2, RoundingMode.HALF_UP)
                val remainingGrams = grams.remainder(BigDecimal(GRAMS_PER_KILOGRAM))
                if (remainingGrams > BigDecimal.ZERO) {
                    "$kilograms kg ${remainingGrams.setScale(0, RoundingMode.HALF_UP)} g"
                } else {
                    "$kilograms kg"
                }
            }
            else -> "${grams.setScale(0, RoundingMode.HALF_UP)} 克"
        }
    }
    
    fun getWeightDescription(grams: Int): String {
        return WeightCategory.fromGrams(grams).label
    }
    
    fun getWeightDescription(grams: BigDecimal): String {
        return getWeightDescription(grams.intValueExact())
    }
    
    fun categorizeByWeight(items: List<EquipmentItem>): Map<WeightCategory, List<EquipmentItem>> {
        return items.groupBy { item ->
            val grams = convertToGrams(item.weight, item.weightUnit)
            WeightCategory.fromGrams(grams.intValueExact())
        }
    }
    
    fun calculateAverageWeight(items: List<EquipmentItem>): BigDecimal {
        return if (items.isEmpty()) {
            BigDecimal.ZERO
        } else {
            val totalWeight = calculateTotalWeight(items)
            totalWeight.divide(BigDecimal(items.size), 2, RoundingMode.HALF_UP)
        }
    }
    
    fun findHeaviestItem(items: List<EquipmentItem>): EquipmentItem? {
        return if (items.isEmpty()) {
            null
        } else {
            items.maxByOrNull { item ->
                convertToGrams(item.weight, item.weightUnit)
            }
        }
    }
    
    fun findLightestItem(items: List<EquipmentItem>): EquipmentItem? {
        return if (items.isEmpty()) {
            null
        } else {
            items.minByOrNull { item ->
                convertToGrams(item.weight, item.weightUnit)
            }
        }
    }
    
    fun getUnitLabel(unitCode: Int): String {
        return WeightUnit.fromCode(unitCode).label
    }
    
    fun getAllUnitOptions(): List<Map<String, Any>> {
        return WeightUnit.values().map { unit ->
            mapOf(
                "code" to unit.code,
                "label" to unit.label,
                "toGramsFactor" to unit.toGramsFactor
            )
        }
    }
}
