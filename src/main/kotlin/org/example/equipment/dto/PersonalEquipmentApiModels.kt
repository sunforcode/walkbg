package org.example.equipment.dto

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter

abstract class EquipmentStrictRequest {
    private val unknownFields = linkedSetOf<String>()

    @JsonAnySetter
    fun captureUnknownField(name: String, value: Any?) {
        unknownFields += name
    }

    fun hasUnknownFields(): Boolean = unknownFields.isNotEmpty()
}

class UnitWeightRequest(
    val grams: Long? = null
) : EquipmentStrictRequest()

class CreatePersonalEquipmentRequest : EquipmentStrictRequest() {
    var name: String? = null
        private set
    var ownedQuantity: Int? = null
        private set
    var unitWeight: UnitWeightRequest? = null
        private set
    var hasName = false
        private set
    var hasOwnedQuantity = false
        private set
    var hasUnitWeight = false
        private set

    @JsonSetter("name")
    fun assignName(value: String?) {
        hasName = true
        name = value
    }

    @JsonSetter("ownedQuantity")
    fun assignOwnedQuantity(value: Int?) {
        hasOwnedQuantity = true
        ownedQuantity = value
    }

    @JsonSetter("unitWeight")
    fun assignUnitWeight(value: UnitWeightRequest?) {
        hasUnitWeight = true
        unitWeight = value
    }
}

class UpdatePersonalEquipmentRequest : EquipmentStrictRequest() {
    var name: String? = null
        private set
    var ownedQuantity: Int? = null
        private set
    var unitWeight: UnitWeightRequest? = null
        private set
    var hasName = false
        private set
    var hasOwnedQuantity = false
        private set
    var hasUnitWeight = false
        private set

    @JsonSetter("name")
    fun assignName(value: String?) {
        hasName = true
        name = value
    }

    @JsonSetter("ownedQuantity")
    fun assignOwnedQuantity(value: Int?) {
        hasOwnedQuantity = true
        ownedQuantity = value
    }

    @JsonSetter("unitWeight")
    fun assignUnitWeight(value: UnitWeightRequest?) {
        hasUnitWeight = true
        unitWeight = value
    }
}

class EquipmentListNameRequest : EquipmentStrictRequest() {
    var name: String? = null
        private set
    var hasName = false
        private set

    @JsonSetter("name")
    fun assignName(value: String?) {
        hasName = true
        name = value
    }
}

class EquipmentListMemberRequest : EquipmentStrictRequest() {
    var quantity: Int? = null
        private set
    var hasQuantity = false
        private set

    @JsonSetter("quantity")
    fun assignQuantity(value: Int?) {
        hasQuantity = true
        quantity = value
    }
}

data class WeightProjection(val grams: Long)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalEquipmentProjection(
    val identity: String,
    val name: String,
    val ownedQuantity: Int,
    val unitWeight: WeightProjection? = null
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
    val quantity: Int
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
