package org.example.equipment.dto

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import org.example.common.contract.ApiContractException

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class EquipmentStrictRequest {
    private val unknownFields = linkedSetOf<String>()
    @JsonAnySetter
    fun captureUnknownField(name: String, value: Any?) { unknownFields += name }
    fun hasUnknownFields(): Boolean = unknownFields.isNotEmpty()
}

class UnitWeightRequest(val grams: Double)

// Presence flags retain the difference between omitted fields and explicit null in PATCH and commands.
open class EquipmentConfigurationRequest : EquipmentStrictRequest() {
    var unitWeight: UnitWeightRequest? = null; private set
    var variant: String? = null; private set
    var unitLabel: String? = null; private set
    var weightScope: String? = null; private set
    var hasUnitWeight = false; private set
    var hasVariant = false; private set
    var hasUnitLabel = false; private set
    var hasWeightScope = false; private set

    @JsonSetter("unitWeight") fun assignUnitWeight(value: JsonNode?) {
        hasUnitWeight = true
        unitWeight = if (value == null || value.isNull) null else {
            if (!value.isObject || value.fieldNames().asSequence().toSet() != setOf("grams") || !value["grams"].isNumber || !value["grams"].asDouble().isFinite()) {
                throw ApiContractException.invalidRequest("unitWeight 必须只包含有限数值 grams")
            }
            UnitWeightRequest(value["grams"].asDouble())
        }
    }
    @JsonSetter("variant") fun assignVariant(value: JsonNode?) { hasVariant = true; variant = equipmentText(value) }
    @JsonSetter("unitLabel") fun assignUnitLabel(value: JsonNode?) { hasUnitLabel = true; unitLabel = equipmentText(value) }
    @JsonSetter("weightScope") fun assignWeightScope(value: JsonNode?) { hasWeightScope = true; weightScope = equipmentText(value) }
}

open class PersonalEquipmentFieldsRequest : EquipmentConfigurationRequest() {
    var name: String? = null; private set
    var ownedQuantity: Int? = null; private set
    var category: String? = null; private set
    var equipmentType: String? = null; private set
    var hasName = false; private set
    var hasOwnedQuantity = false; private set
    var hasCategory = false; private set
    var hasEquipmentType = false; private set
    @JsonSetter("name") fun assignName(value: JsonNode?) { hasName = true; name = equipmentText(value) }
    @JsonSetter("ownedQuantity") fun assignOwnedQuantity(value: JsonNode?) { hasOwnedQuantity = true; ownedQuantity = equipmentInteger(value) }
    @JsonSetter("category") fun assignCategory(value: JsonNode?) { hasCategory = true; category = equipmentText(value) }
    @JsonSetter("equipmentType") fun assignEquipmentType(value: JsonNode?) { hasEquipmentType = true; equipmentType = equipmentText(value) }
}

class CreatePersonalEquipmentRequest : PersonalEquipmentFieldsRequest()
class UpdatePersonalEquipmentRequest : PersonalEquipmentFieldsRequest()

class AdoptCatalogEquipmentRequest : EquipmentConfigurationRequest() {
    var catalogEquipmentId: String? = null; private set
    var ownedQuantity: Int? = null; private set
    var name: String? = null; private set
    var hasName = false; private set
    @JsonSetter("catalogEquipmentId") fun assignCatalogEquipmentId(value: JsonNode?) { catalogEquipmentId = equipmentText(value) }
    @JsonSetter("ownedQuantity") fun assignOwnedQuantity(value: JsonNode?) { ownedQuantity = equipmentInteger(value) }
    @JsonSetter("name") fun assignName(value: JsonNode?) { hasName = true; name = equipmentText(value) }
}

class EquipmentListNameRequest : EquipmentStrictRequest() {
    var name: String? = null; private set
    var hasName = false; private set
    @JsonSetter("name") fun assignName(value: JsonNode?) { hasName = true; name = equipmentText(value) }
}

class EquipmentListMemberRequest : EquipmentStrictRequest() {
    var quantity: Int? = null; private set
    var hasQuantity = false; private set
    @JsonSetter("quantity") fun assignQuantity(value: JsonNode?) { hasQuantity = true; quantity = equipmentInteger(value) }
}

private fun equipmentText(value: JsonNode?): String? {
    if (value == null || value.isNull) return null
    if (!value.isTextual) throw ApiContractException.invalidRequest("字段必须为字符串")
    return value.textValue()
}

private fun equipmentInteger(value: JsonNode?): Int? {
    if (value == null || value.isNull) return null
    if (!value.isIntegralNumber || !value.canConvertToInt()) throw ApiContractException.invalidRequest("数量必须为整数")
    return value.intValue()
}

data class WeightProjection(val grams: Double)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalEquipmentProjection(
    val identity: String,
    val name: String,
    val ownedQuantity: Int,
    val unitWeight: WeightProjection? = null,
    val category: String? = null,
    val equipmentType: String? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null,
    val unitWeightSource: String? = null,
    val catalogEquipmentId: String? = null
)

data class PersonalEquipmentSummary(
    val equipmentItemCount: Int,
    val equipmentListCount: Int,
    val knownTotalWeight: WeightProjection,
    val missingWeightItemCount: Int
)

data class PersonalEquipmentCollectionResponse(
    val items: List<PersonalEquipmentProjection>,
    val summary: PersonalEquipmentSummary
)

data class PersonalEquipmentMutationResponse(
    val item: PersonalEquipmentProjection,
    val summary: PersonalEquipmentSummary
)

data class EquipmentListReferenceProjection(
    val identity: String,
    val name: String
)

data class PersonalEquipmentDeletionImpact(
    val personalEquipmentId: String,
    val equipmentListReferenceCount: Int,
    val affectedEquipmentLists: List<EquipmentListReferenceProjection>
)

data class PersonalEquipmentDeletionResponse(
    val deletedPersonalEquipmentId: String,
    val removedEquipmentListReferenceCount: Int,
    val summary: PersonalEquipmentSummary
)

data class EquipmentListSummary(
    val itemCount: Int,
    val knownTotalWeight: WeightProjection,
    val missingWeightItemCount: Int
)

data class EquipmentListSummaryProjection(
    val identity: String,
    val name: String,
    val summary: EquipmentListSummary
)

data class EquipmentListCollectionResponse(
    val items: List<EquipmentListSummaryProjection>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EquipmentListMemberProjection(
    val identity: String,
    val name: String,
    val ownedQuantity: Int,
    val unitWeight: WeightProjection? = null,
    val quantity: Int,
    val category: String? = null,
    val equipmentType: String? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null,
    val unitWeightSource: String? = null,
    val catalogEquipmentId: String? = null
)

data class EquipmentListDetailProjection(
    val identity: String,
    val name: String,
    val members: List<EquipmentListMemberProjection>,
    val summary: EquipmentListSummary
)

data class EquipmentListCreateResponse(
    val equipmentList: EquipmentListDetailProjection,
    val personalEquipmentSummary: PersonalEquipmentSummary
)

data class EquipmentListMutationResponse(
    val equipmentList: EquipmentListDetailProjection
)

data class EquipmentListDeletionResponse(
    val deletedEquipmentListId: String,
    val personalEquipmentSummary: PersonalEquipmentSummary
)
