package org.example.trip.personal.repository

import org.example.trip.personal.model.PersonalTripDayRecord
import org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord
import org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord
import org.example.trip.personal.model.PersonalTripIdempotencyRecord
import org.example.trip.personal.model.PersonalTripOwnership
import org.example.trip.personal.model.PersonalTripRecord
import org.example.trip.personal.model.TripFrozenRouteVersion
import org.example.trip.personal.model.TripSuppressesEquipmentSuggestion
import org.example.trip.personal.model.TripSuppressesEquipmentSuggestionId
import org.example.trip.personal.model.TripTransportSelectionRecord
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PersonalTripRepository : JpaRepository<PersonalTripRecord, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select trip from PersonalTripRecord trip where trip.id = :tripId")
    fun findByIdForUpdate(@Param("tripId") tripId: String): PersonalTripRecord?
}

interface PersonalTripOwnershipRepository : JpaRepository<PersonalTripOwnership, String> {
    fun findByAccountIdOrderByTripIdAsc(accountId: String): List<PersonalTripOwnership>
    fun findByTripIdAndAccountId(tripId: String, accountId: String): PersonalTripOwnership?
}

interface TripFrozenRouteVersionRepository : JpaRepository<TripFrozenRouteVersion, String>

interface PersonalTripDayRepository : JpaRepository<PersonalTripDayRecord, String> {
    fun findByTripIdOrderByDayNumberAsc(tripId: String): List<PersonalTripDayRecord>
    fun deleteByTripId(tripId: String): Long
}

interface PersonalTripEquipmentSnapshotRepository : JpaRepository<PersonalTripEquipmentSnapshotRecord, String> {
    fun findByTripId(tripId: String): PersonalTripEquipmentSnapshotRecord?
    fun deleteByTripId(tripId: String): Long
}

interface PersonalTripEquipmentItemRepository : JpaRepository<PersonalTripEquipmentItemRecord, String> {
    fun findBySnapshotIdOrderByDisplayOrderAsc(snapshotId: String): List<PersonalTripEquipmentItemRecord>
    fun findByIdAndSnapshotId(id: String, snapshotId: String): PersonalTripEquipmentItemRecord?
    fun existsBySnapshotIdAndNormalizedName(snapshotId: String, normalizedName: String): Boolean
    fun existsBySnapshotIdAndNormalizedNameAndIdNot(
        snapshotId: String,
        normalizedName: String,
        id: String
    ): Boolean
    fun deleteBySnapshotId(snapshotId: String): Long
}

interface PersonalTripEquipmentItemDerivationRepository :
    JpaRepository<PersonalTripEquipmentItemDerivedFromSuggestion, String> {
    fun findByItemId(itemId: String): PersonalTripEquipmentItemDerivedFromSuggestion?
    fun findByTripIdOrderByItemIdAsc(tripId: String): List<PersonalTripEquipmentItemDerivedFromSuggestion>
    fun deleteByTripId(tripId: String): Long
}

interface PersonalTripEquipmentSuppressionRepository :
    JpaRepository<TripSuppressesEquipmentSuggestion, TripSuppressesEquipmentSuggestionId> {
    fun findByTripIdAndLogicalSuggestionId(
        tripId: String,
        logicalSuggestionId: String
    ): TripSuppressesEquipmentSuggestion?
    fun findByTripIdOrderByLogicalSuggestionIdAsc(tripId: String): List<TripSuppressesEquipmentSuggestion>
}

interface PersonalTripIdempotencyRepository : JpaRepository<PersonalTripIdempotencyRecord, String> {
    fun findByAccountIdAndOperationAndIdempotencyKey(
        accountId: String,
        operation: String,
        idempotencyKey: String
    ): PersonalTripIdempotencyRecord?

    @Modifying
    @Query(
        value = """
            INSERT IGNORE INTO personal_trip_idempotency (
                id, account_id, operation_name, idempotency_key, request_hash,
                response_type, response_json, created_at
            ) VALUES (
                :id, :accountId, :operation, :idempotencyKey, :requestHash,
                :responseType, :responseJson, CURRENT_TIMESTAMP(6)
            )
        """,
        nativeQuery = true
    )
    fun insertReservation(
        @Param("id") id: String,
        @Param("accountId") accountId: String,
        @Param("operation") operation: String,
        @Param("idempotencyKey") idempotencyKey: String,
        @Param("requestHash") requestHash: String,
        @Param("responseType") responseType: String,
        @Param("responseJson") responseJson: String
    ): Int

    @Query(
        value = """
            SELECT * FROM personal_trip_idempotency
            WHERE account_id = :accountId
              AND operation_name = :operation
              AND idempotency_key = :idempotencyKey
            FOR UPDATE
        """,
        nativeQuery = true
    )
    fun findClaimForUpdate(
        @Param("accountId") accountId: String,
        @Param("operation") operation: String,
        @Param("idempotencyKey") idempotencyKey: String
    ): PersonalTripIdempotencyRecord?

    @Modifying
    @Query(
        value = """
            UPDATE personal_trip_idempotency
            SET response_type = :responseType, response_json = :responseJson
            WHERE account_id = :accountId
              AND operation_name = :operation
              AND idempotency_key = :idempotencyKey
              AND request_hash = :requestHash
              AND response_type = 'pending'
        """,
        nativeQuery = true
    )
    fun completeReservation(
        @Param("accountId") accountId: String,
        @Param("operation") operation: String,
        @Param("idempotencyKey") idempotencyKey: String,
        @Param("requestHash") requestHash: String,
        @Param("responseType") responseType: String,
        @Param("responseJson") responseJson: String
    ): Int
}

interface TripTransportSelectionRepository : JpaRepository<TripTransportSelectionRecord, String>
