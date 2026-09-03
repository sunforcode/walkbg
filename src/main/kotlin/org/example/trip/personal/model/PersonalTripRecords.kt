package org.example.trip.personal.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "personal_trips",
    indexes = [
        Index(name = "idx_personal_trips_start_date", columnList = "start_date"),
        Index(name = "idx_personal_trips_end_date", columnList = "end_date")
    ]
)
data class PersonalTripRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "first_generated_at", nullable = false, updatable = false)
    val firstGeneratedAt: Instant,

    @Column(name = "lifecycle_state", nullable = false, length = 32)
    var lifecycleState: String = "active",

    @Column(name = "departure_city", nullable = false, length = 200)
    val departureCity: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "total_day_count", nullable = false)
    var totalDayCount: Int,

    @Column(name = "hiking_day_count", nullable = false)
    var hikingDayCount: Int,

    @Column(nullable = false, length = 64)
    var revision: String,

    @Column(name = "frozen_route_basis_json", nullable = false, columnDefinition = "LONGTEXT")
    var frozenRouteBasisJson: String,

    @Column(name = "weather_overview_json", columnDefinition = "LONGTEXT")
    var weatherOverviewJson: String? = null,

    @Column(name = "important_notices_json", columnDefinition = "LONGTEXT")
    var importantNoticesJson: String? = null,

    @Column(name = "selected_transport_option_id", length = 64)
    var selectedTransportOptionId: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = firstGeneratedAt
)

@Entity
@Table(
    name = "personal_trip_ownership",
    indexes = [Index(name = "idx_personal_trip_owner", columnList = "account_id")]
)
data class PersonalTripOwnership(
    @Id
    @Column(name = "trip_id", length = 64)
    val tripId: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String
)

@Entity
@Table(name = "trip_frozen_route_versions")
data class TripFrozenRouteVersion(
    @Id
    @Column(name = "trip_id", length = 64)
    val tripId: String,

    @Column(name = "route_version_id", nullable = false, length = 64)
    var routeVersionId: String
)

@Entity
@Table(
    name = "personal_trip_days",
    indexes = [Index(name = "idx_personal_trip_days_trip", columnList = "trip_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_personal_trip_day_number", columnNames = ["trip_id", "day_number"])]
)
data class PersonalTripDayRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "trip_id", nullable = false, length = 64)
    val tripId: String,

    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(name = "primary_stage", nullable = false, length = 100)
    val primaryStage: String,

    @Column(name = "hiking_day_number")
    val hikingDayNumber: Int? = null,

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    val contentJson: String
)

@Entity
@Table(name = "personal_trip_equipment_snapshots")
data class PersonalTripEquipmentSnapshotRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "trip_id", nullable = false, unique = true, length = 64)
    val tripId: String,

    @Column(name = "item_count", nullable = false)
    var itemCount: Int,

    @Column(name = "known_total_weight_grams", nullable = false)
    var knownTotalWeightGrams: Long,

    @Column(name = "missing_weight_item_count", nullable = false)
    var missingWeightItemCount: Int,

    @Column(name = "owned_item_count", nullable = false)
    var ownedItemCount: Int,

    @Column(name = "unconfirmed_owned_item_count", nullable = false)
    var unconfirmedOwnedItemCount: Int
)

@Entity
@Table(
    name = "personal_trip_equipment_items",
    indexes = [Index(name = "idx_personal_trip_equipment_items_snapshot", columnList = "snapshot_id")],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_personal_trip_item_order", columnNames = ["snapshot_id", "display_order"]),
        UniqueConstraint(
            name = "uk_personal_trip_item_normalized_name",
            columnNames = ["snapshot_id", "normalized_name"]
        )
    ]
)
data class PersonalTripEquipmentItemRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "snapshot_id", nullable = false, length = 64)
    val snapshotId: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "normalized_name", nullable = false, length = 200)
    var normalizedName: String,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "unit_weight_grams")
    var unitWeightGrams: Long? = null,

    @Column(nullable = false, length = 32)
    var source: String,

    @Column(name = "ownership_status", nullable = false, length = 32)
    var ownershipStatus: String,

    @Column(columnDefinition = "TEXT")
    var note: String? = null
)

@Entity
@Table(
    name = "personal_trip_equipment_item_derivations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_trip_equipment_derivation_logical",
            columnNames = ["trip_id", "logical_suggestion_id"]
        )
    ],
    indexes = [Index(name = "idx_trip_equipment_derivation_trip", columnList = "trip_id")]
)
data class PersonalTripEquipmentItemDerivedFromSuggestion(
    @Id
    @Column(name = "item_id", length = 64)
    val itemId: String,

    @Column(name = "trip_id", nullable = false, length = 64)
    val tripId: String,

    @Column(name = "logical_suggestion_id", nullable = false, length = 64)
    val logicalSuggestionId: String,

    @Column(name = "suggestion_occurrence_id", length = 64)
    val suggestionOccurrenceId: String? = null,

    @Column(name = "route_id", nullable = false, length = 64)
    val routeId: String = ""
)

@Entity
@Table(name = "personal_trip_equipment_suppressions")
@jakarta.persistence.IdClass(TripSuppressesEquipmentSuggestionId::class)
data class TripSuppressesEquipmentSuggestion(
    @Id
    @Column(name = "trip_id", length = 64)
    val tripId: String,

    @Id
    @Column(name = "logical_suggestion_id", length = 64)
    val logicalSuggestionId: String,

    @Column(name = "route_id", nullable = false, length = 64)
    val routeId: String = ""
)

data class TripSuppressesEquipmentSuggestionId(
    val tripId: String = "",
    val logicalSuggestionId: String = ""
) : java.io.Serializable

@Entity
@Table(
    name = "personal_trip_idempotency",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_personal_trip_idempotency",
            columnNames = ["account_id", "operation_name", "idempotency_key"]
        )
    ]
)
data class PersonalTripIdempotencyRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String,

    @Column(name = "operation_name", nullable = false, length = 64)
    val operation: String,

    @Column(name = "idempotency_key", nullable = false, length = 200)
    val idempotencyKey: String,

    @Column(name = "request_hash", nullable = false, length = 64)
    val requestHash: String,

    @Column(name = "response_type", nullable = false, length = 100)
    val responseType: String,

    @Column(name = "response_json", nullable = false, columnDefinition = "LONGTEXT")
    val responseJson: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(
    name = "trip_transport_selections",
    indexes = [Index(name = "idx_trip_transport_selection_account", columnList = "account_id")]
)
data class TripTransportSelectionRecord(
    @Id
    @Column(name = "selection_id", length = 64)
    val selectionId: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String,

    @Column(name = "request_hash", nullable = false, length = 64)
    val requestHash: String,

    @Column(name = "context_json", nullable = false, columnDefinition = "LONGTEXT")
    val contextJson: String,

    @Column(name = "options_json", nullable = false, columnDefinition = "LONGTEXT")
    val optionsJson: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
