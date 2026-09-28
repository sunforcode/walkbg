package org.example.equipment.service

import org.example.common.contract.ApiContractException
import org.springframework.stereotype.Service
import org.example.equipment.catalog.model.CatalogType
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.util.normalizeEquipmentSearchText

@Service
class PersonalEquipmentDomainService {
    data class NormalizedName(val display: String, val comparison: String)

    fun normalizeEquipmentName(input: String?): NormalizedName {
        if (input == null) {
            throw ApiContractException.unprocessable("validation_failed", "装备名称不能为空")
        }
        val display = collapseWhitespace(input)
        if (display.isEmpty()) {
            throw ApiContractException.unprocessable("validation_failed", "装备名称不能为空")
        }
        if (display.length > 200) {
            throw ApiContractException.unprocessable("validation_failed", "装备名称不能超过 200 个字符")
        }
        return NormalizedName(display, foldAsciiCase(display))
    }

    fun normalizeListName(input: String?): String {
        if (input == null) {
            throw ApiContractException.unprocessable("validation_failed", "清单名称不能为空")
        }
        val name = collapseWhitespace(input)
        if (name.isEmpty()) {
            throw ApiContractException.unprocessable("validation_failed", "清单名称不能为空")
        }
        if (name.length > 200) {
            throw ApiContractException.unprocessable("validation_failed", "清单名称不能超过 200 个字符")
        }
        return name
    }

    fun requirePositiveQuantity(quantity: Int?, fieldName: String = "数量"): Int {
        if (quantity == null || quantity <= 0) {
            throw ApiContractException.unprocessable("validation_failed", "${fieldName}必须为正整数")
        }
        return quantity
    }

    fun requirePositiveWeight(grams: Double?): Double {
        if (grams != null && !grams.isFinite()) throw ApiContractException.invalidRequest("重量必须为有限数值")
        if (grams == null || grams <= 0.0) {
            throw ApiContractException.unprocessable("validation_failed", "单件重量必须为正克重")
        }
        return grams
    }

    fun optionalText(value: String?): String? = value?.trim { it.isWhitespace() || Character.isSpaceChar(it) }?.also {
        if (it.isEmpty()) throw ApiContractException.unprocessable("validation_failed", "配置说明不能为空")
    }

    fun validateConfiguration(record: PersonalEquipmentRecord) {
        val type = record.equipmentType?.let { CatalogType.fromCode(it) }
        if ((record.category != null && record.category !in CatalogType.categories) ||
            (record.equipmentType != null && (type == null || record.category !in type.allowedCategories)) ||
            (record.unitLabel != null && record.unitLabel !in CatalogType.unitLabels)) {
            throw ApiContractException.unprocessable("validation_failed", "装备分类、类型或计数单位无效")
        }
        record.unitWeightGrams?.let(::requirePositiveWeight)
    }

    fun validateFilter(filter: PersonalEquipmentFilter) {
        val type = filter.equipmentType?.let { CatalogType.fromCode(it) ?: invalidFilter() }
        if (filter.category != null && filter.category !in CatalogType.categories) invalidFilter()
        if (type != null && filter.category != null && filter.category !in type.allowedCategories) invalidFilter()
        if (filter.keyword != null && normalizeEquipmentSearchText(filter.keyword).isEmpty()) invalidFilter()
    }

    private fun invalidFilter(): Nothing = throw ApiContractException.invalidRequest("个人装备筛选参数无效")

    private fun collapseWhitespace(value: String): String {
        val result = StringBuilder()
        var pendingSpace = false
        value.codePoints().forEach { codePoint ->
            if (isCommonWhitespace(codePoint)) {
                if (result.isNotEmpty()) pendingSpace = true
            } else {
                if (pendingSpace) result.append(' ')
                result.appendCodePoint(codePoint)
                pendingSpace = false
            }
        }
        return result.toString()
    }

    private fun isCommonWhitespace(codePoint: Int): Boolean =
        Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)

    private fun foldAsciiCase(value: String): String = buildString(value.length) {
        value.forEach { character ->
            append(if (character in 'A'..'Z') character + ('a' - 'A') else character)
        }
    }
}
