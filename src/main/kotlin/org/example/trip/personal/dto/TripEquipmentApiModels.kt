package org.example.trip.personal.dto

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import org.example.equipment.dto.WeightProjection
import java.time.LocalDate

abstract class TripEquipmentStrictRequest {
    private val unknownFields = linkedSetOf<String>()

    @JsonAnySetter
    fun captureUnknownField(name: String, value: Any?) {
        unknownFields += name
    }

    fun hasUnknownFields(): Boolean = unknownFields.isNotEmpty()
}

class TripEquipmentWeightRequest(
    val grams: Long? = null
) : TripEquipmentStrictRequest()

class CreateTripEquipmentItemRequest : TripEquipmentStrictRequest() {
    var name: String? = null
        private set
    var quantity: Int? = null
        private set
    var unitWeight: TripEquipmentWeightRequest? = null
        private set
    var note: String? = null
        private set
    var hasName = false
        private set
    var hasQuantity = false
        private set
    var hasUnitWeight = false
        private set
    var hasNote = false
        private set

    @JsonSetter("name")
    fun assignName(value: String?) {
        hasName = true
        name = value
    }

    @JsonSetter("quantity")
    fun assignQuantity(value: Int?) {
        hasQuantity = true
        quantity = value
    }

    @JsonSetter("unitWeight")
    fun assignUnitWeight(value: TripEquipmentWeightRequest?) {
        hasUnitWeight = true
        unitWeight = value
    }

    @JsonSetter("note")
    fun assignNote(value: String?) {
        hasNote = true
        note = value
    }
}

class UpdateTripEquipmentItemRequest : TripEquipmentStrictRequest() {
    var name: String? = null
        private set
    var quantity: Int? = null
        private set
    var unitWeight: TripEquipmentWeightRequest? = null
        private set
    var note: String? = null
        private set
    var hasName = false
        private set
    var hasQuantity = false
        private set
    var hasUnitWeight = false
        private set
    var hasNote = false
        private set

    @JsonSetter("name")
    fun assignName(value: String?) {
        hasName = true
        name = value
    }

    @JsonSetter("quantity")
    fun assignQuantity(value: Int?) {
        hasQuantity = true
        quantity = value
    }

    @JsonSetter("unitWeight")
    fun assignUnitWeight(value: TripEquipmentWeightRequest?) {
        hasUnitWeight = true
        unitWeight = value
    }

    @JsonSetter("note")
    fun assignNote(value: String?) {
        hasNote = true
        note = value
    }
}

data class CreateTripEquipmentItemCommand(
    val name: String?,
    val quantity: Int?,
    val unitWeightGrams: Long?,
    val note: String?
)

data class UpdateTripEquipmentItemCommand(
    val name: String?,
    val quantity: Int?,
    val unitWeightGrams: Long?,
    val note: String?,
    val hasName: Boolean,
    val hasQuantity: Boolean,
    val hasUnitWeight: Boolean,
    val hasNote: Boolean
)

data class TripEquipmentTripProjection(
    val identity: String,
    val status: String,
    val routeName: String,
    @BusinessDate val startDate: LocalDate,
    @BusinessDate val endDate: LocalDate
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripEquipmentItemProjection(
    val identity: String,
    val name: String,
    val quantity: Int,
    val unitWeight: WeightProjection? = null,
    val note: String? = null,
    val source: String,
    val ownershipStatus: String
)

data class TripEquipmentSnapshotProjection(
    val identity: String,
    val items: List<TripEquipmentItemProjection>,
    val summary: TripEquipmentSummaryProjection
)

data class TripEquipmentProjection(
    val trip: TripEquipmentTripProjection,
    val revision: String,
    val editable: Boolean,
    val snapshot: TripEquipmentSnapshotProjection
)
