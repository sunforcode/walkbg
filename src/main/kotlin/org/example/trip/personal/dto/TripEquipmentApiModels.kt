package org.example.trip.personal.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.example.equipment.dto.WeightProjection
import java.time.LocalDate

data class TripCarryAllocation(val backpackQuantity: Int, val wornQuantity: Int, val handheldQuantity: Int)
data class TripSupplyAmount(val value: Double, val unit: String)
data class TripSupply(val kind: String, val amount: TripSupplyAmount, val containerMode: String)

/** Optional concrete configuration copied into this trip; not a reference to personal equipment. */
data class TripItemConfiguration(
    val category: String? = null,
    val equipmentType: String? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null,
    val carryAllocation: TripCarryAllocation? = null,
    val supply: TripSupply? = null
)

data class CreateTripEquipmentItemCommand(
    val name: String?, val quantity: Int?, val unitWeightGrams: Double?, val note: String?,
    val configuration: TripItemConfiguration = TripItemConfiguration()
)

data class UpdateTripEquipmentItemCommand(
    val name: String?, val quantity: Int?, val unitWeightGrams: Double?, val note: String?,
    val hasName: Boolean, val hasQuantity: Boolean, val hasUnitWeight: Boolean, val hasNote: Boolean,
    val configuration: TripItemConfiguration = TripItemConfiguration(),
    val configurationFields: Set<String> = emptySet()
)

data class SelectPersonalEquipmentCommand(val personalEquipmentId: String, val quantity: Int)
data class AppendEquipmentListCommand(val equipmentListId: String)

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
    val ownershipStatus: String,
    val category: String? = null,
    val equipmentType: String? = null,
    val variant: String? = null,
    val unitLabel: String? = null,
    val weightScope: String? = null,
    val carryAllocation: TripCarryAllocation? = null,
    val supply: TripSupply? = null,
    val packedQuantity: Int? = null,
    val ownershipCheckApplicable: Boolean? = null
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

data class EquipmentWeightBucket(val knownWeight: WeightProjection, val missingWeightItemCount: Int)
data class TripWeightBreakdown(
    val backpack: EquipmentWeightBucket, val worn: EquipmentWeightBucket,
    val handheld: EquipmentWeightBucket, val unallocated: EquipmentWeightBucket
)
data class TripBackpackBreakdown(
    val equipment: EquipmentWeightBucket, val food: EquipmentWeightBucket,
    val drinkingWater: EquipmentWeightBucket, val fuel: EquipmentWeightBucket,
    val unclassified: EquipmentWeightBucket
)
data class TripPackingProgress(
    val backpackQuantity: Long, val packedQuantity: Long, val unpackedQuantity: Long,
    val unallocatedItemCount: Int
)
data class TripPreparationProjection(
    val tripId: String, val revision: String, val weightBreakdown: TripWeightBreakdown,
    val backpackBreakdown: TripBackpackBreakdown, val packing: TripPackingProgress
)
