package org.example.equipment.service

import org.example.common.contract.ApiContractException
import org.springframework.stereotype.Service

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

    fun requirePositiveWeight(grams: Long?): Long {
        if (grams == null || grams <= 0) {
            throw ApiContractException.unprocessable("validation_failed", "单件重量必须为正克重")
        }
        return grams
    }

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
