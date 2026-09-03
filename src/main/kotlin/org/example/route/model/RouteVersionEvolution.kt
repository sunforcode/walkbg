package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "route_version_publication_order")
@IdClass(RouteVersionPublicationOrderKey::class)
data class RouteVersionPublicationOrder(
    @Id
    @Column(name = "route_id", nullable = false, length = 64)
    val routeId: String,

    @Id
    @Column(name = "route_version_id", nullable = false, length = 64)
    val routeVersionId: String,

    @Column(name = "published_sequence", nullable = false)
    val publishedSequence: Int
)

data class RouteVersionPublicationOrderKey(
    val routeId: String = "",
    val routeVersionId: String = ""
) : Serializable

@Entity
@Table(name = "logical_equipment_suggestion_identities")
@IdClass(LogicalEquipmentSuggestionIdentityKey::class)
data class LogicalEquipmentSuggestionIdentity(
    @Id
    @Column(name = "route_id", nullable = false, length = 64)
    val routeId: String,

    @Id
    @Column(name = "logical_suggestion_id", nullable = false, length = 64)
    val logicalSuggestionId: String
)

data class LogicalEquipmentSuggestionIdentityKey(
    val routeId: String = "",
    val logicalSuggestionId: String = ""
) : Serializable

@Entity
@Table(name = "route_version_equipment_suggestions")
data class RouteVersionEquipmentSuggestion(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", nullable = false, length = 64)
    val routeId: String,

    @Column(name = "route_version_id", nullable = false, length = 64)
    val routeVersionId: String,

    @Column(name = "logical_suggestion_id", nullable = false, length = 64)
    val logicalSuggestionId: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(nullable = false, length = 200)
    val name: String,

    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    @Column(nullable = false)
    val quantity: Int,

    @Column(name = "unit_weight_grams")
    val unitWeightGrams: Long? = null,

    @Column(length = 500)
    val note: String? = null,

    @Column(nullable = false, length = 32)
    val level: String
)
