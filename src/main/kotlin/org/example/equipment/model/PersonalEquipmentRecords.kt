package org.example.equipment.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "personal_equipment")
data class PersonalEquipmentRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "owned_quantity", nullable = false)
    var ownedQuantity: Int,

    @Column(name = "unit_weight_grams")
    var unitWeightGrams: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(
    name = "personal_equipment_ownership",
    indexes = [Index(name = "idx_personal_equipment_owner", columnList = "account_id")]
)
data class PersonalEquipmentOwnership(
    @Id
    @Column(name = "personal_equipment_id", length = 64)
    val personalEquipmentId: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String,

    @Column(name = "normalized_name", nullable = false, length = 200)
    var normalizedName: String
)

@Entity
@Table(name = "user_equipment_lists")
data class UserEquipmentListRecord(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(
    name = "equipment_list_ownership",
    indexes = [Index(name = "idx_equipment_list_owner", columnList = "account_id")]
)
data class EquipmentListOwnership(
    @Id
    @Column(name = "equipment_list_id", length = 64)
    val equipmentListId: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String
)

@Entity
@Table(
    name = "equipment_list_members",
    indexes = [Index(name = "idx_equipment_list_member_equipment", columnList = "personal_equipment_id")]
)
@IdClass(EquipmentListMemberId::class)
data class EquipmentListMember(
    @Id
    @Column(name = "equipment_list_id", length = 64)
    val equipmentListId: String,

    @Id
    @Column(name = "personal_equipment_id", length = 64)
    val personalEquipmentId: String,

    @Column(nullable = false)
    var quantity: Int
)

data class EquipmentListMemberId(
    val equipmentListId: String = "",
    val personalEquipmentId: String = ""
) : Serializable
