package org.example.trip.personal.dto

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.equipment.dto.EquipmentListSummary
import org.example.equipment.dto.PersonalEquipmentSummary
import org.example.equipment.dto.WeightProjection
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.dto.PublicRoutePlace
import org.example.route.dto.RouteGenerationEligibility
import org.example.route.dto.RouteMeters
import org.example.route.dto.RouteSeconds
import java.time.Instant
import java.time.LocalDate

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
@JacksonAnnotationsInside
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
annotation class BusinessDate

data class GenerateTripCommand(
    val routeId: String,
    val routeVersionId: String,
    val departureCity: String,
    @BusinessDate val startDate: LocalDate,
    val equipmentListId: String? = null,
    val transportSelection: TransportSelectionCommand? = null
)

data class TransportSelectionCommand(
    val selectionId: String,
    val transportOptionId: String
)

data class CancelTripCommand(
    val expectedRevision: String,
    val confirmed: Boolean
)

data class MigrateTripCommand(
    val expectedRevision: String,
    val targetRouteVersionId: String
)

data class MigrationTargetChangedDetails(
    val requestedTargetRouteVersionId: String,
    val currentPublicRouteVersionId: String?
)

open class PersonalTripStrictRequest {
    private val unknownFields = linkedSetOf<String>()

    @JsonAnySetter
    fun captureUnknownField(name: String, value: Any?) {
        unknownFields += name
    }

    fun hasUnknownFields(): Boolean = unknownFields.isNotEmpty()
}

class GenerateTripRequest(
    val routeId: String? = null,
    val routeVersionId: String? = null,
    val departureCity: String? = null,
    @BusinessDate val startDate: LocalDate? = null,
    val equipmentListId: String? = null,
    val transportSelection: TransportSelectionRequest? = null
) : PersonalTripStrictRequest()

class TransportSelectionRequest(
    val selectionId: String? = null,
    val transportOptionId: String? = null
) : PersonalTripStrictRequest()

class CancelTripRequest(
    val expectedRevision: String? = null,
    val confirmed: Boolean? = null
) : PersonalTripStrictRequest()

class MigrateTripRequest(
    val expectedRevision: String? = null,
    val targetRouteVersionId: String? = null
) : PersonalTripStrictRequest()

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InformationConfidenceProjection(
    val status: String,
    val category: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QualifiedValueProjection<T>(
    val value: T? = null,
    val confidence: InformationConfidenceProjection? = null
)

data class CalendarDayProjection(
    @BusinessDate val date: LocalDate,
    val tripCount: Int
)

data class PersonalTripCalendarProjection(
    @BusinessDate val windowStartDate: LocalDate,
    @BusinessDate val windowEndDate: LocalDate,
    val days: List<CalendarDayProjection>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FrozenRouteBasisProjection(
    val routeName: String,
    val routeType: String,
    val region: String,
    val start: PublicRoutePlace,
    val end: PublicRoutePlace,
    val estimatedDuration: RouteSeconds,
    val mainTrackPath: List<PublicRouteGeoPosition>,
    val versionLabel: String? = null,
    val cover: String? = null,
    val direction: String? = null,
    val distance: RouteMeters? = null,
    val ascent: RouteMeters? = null,
    val descent: RouteMeters? = null,
    val maxElevation: RouteMeters? = null
)

data class TripRouteReference(
    val routeId: String,
    val adoptedRouteVersionId: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalTripProjection(
    val identity: String,
    val name: String,
    val firstGeneratedAt: Instant,
    val status: String,
    val departureCity: String,
    @BusinessDate val startDate: LocalDate,
    @BusinessDate val endDate: LocalDate,
    val totalDayCount: Int,
    val hikingDayCount: Int,
    val frozenRouteBasis: FrozenRouteBasisProjection,
    val weatherOverview: QualifiedValueProjection<String>? = null,
    val importantNotices: List<ImportantNoticeProjection>? = null
)

data class ImportantNoticeProjection(
    val sequence: Int,
    val type: String,
    val content: QualifiedValueProjection<String>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripSummaryProjection(
    val tripId: String,
    val revision: String,
    val name: String,
    val status: String,
    val routeReference: TripRouteReference,
    val routeName: String,
    val routeType: String,
    val start: PublicRoutePlace,
    val end: PublicRoutePlace,
    val estimatedDuration: RouteSeconds,
    @BusinessDate val startDate: LocalDate,
    @BusinessDate val endDate: LocalDate,
    val totalDayCount: Int,
    val hikingDayCount: Int,
    val versionLabel: String? = null,
    val cover: String? = null,
    val direction: String? = null,
    val distance: RouteMeters? = null,
    val ascent: RouteMeters? = null
)

data class PersonalTripFocusProjection(val trip: TripSummaryProjection?)

data class PersonalTripCollectionProjection(
    val currentTrips: List<TripSummaryProjection>,
    val historicalTrips: List<TripSummaryProjection>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RouteSectionSnapshotProjection(
    val start: PublicRoutePlace,
    val end: PublicRoutePlace,
    val name: String? = null,
    val path: List<PublicRouteGeoPosition>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripActionProjection(
    val sequence: Int,
    val actionType: String,
    val origin: QualifiedValueProjection<PublicRoutePlace>? = null,
    val destination: QualifiedValueProjection<PublicRoutePlace>? = null,
    val mode: QualifiedValueProjection<String>? = null,
    val keyTimes: QualifiedValueProjection<List<String>>? = null,
    val estimatedDuration: QualifiedValueProjection<RouteSeconds>? = null,
    val transferNotes: QualifiedValueProjection<List<String>>? = null,
    val routeSectionSnapshot: RouteSectionSnapshotProjection? = null,
    val start: QualifiedValueProjection<PublicRoutePlace>? = null,
    val end: QualifiedValueProjection<PublicRoutePlace>? = null,
    val distance: QualifiedValueProjection<RouteMeters>? = null,
    val ascent: QualifiedValueProjection<RouteMeters>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripDayWeatherProjection(
    val condition: QualifiedValueProjection<Any>,
    val temperatureRange: QualifiedValueProjection<Any>,
    val precipitation: QualifiedValueProjection<Any>,
    val wind: QualifiedValueProjection<Any>,
    val placeContext: QualifiedValueProjection<Any>,
    val routeSectionContext: QualifiedValueProjection<Any>? = null,
    val source: QualifiedValueProjection<String>? = null,
    val updatedAt: QualifiedValueProjection<Instant>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripPointProjection(
    val sequence: Int,
    val category: String,
    val type: String,
    val name: String? = null,
    val distance: QualifiedValueProjection<RouteMeters>? = null,
    val location: QualifiedValueProjection<PublicRouteGeoPosition>? = null,
    val details: QualifiedValueProjection<String>? = null,
    val isAlternative: Boolean? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripDayProjection(
    val identity: String,
    val dayNumber: Int,
    @BusinessDate val date: LocalDate,
    val primaryStage: String,
    val hikingDayNumber: Int? = null,
    val actions: List<TripActionProjection>,
    val weather: TripDayWeatherProjection,
    val points: List<TripPointProjection>? = null
)

data class TripEquipmentSummaryProjection(
    val itemCount: Int,
    val knownTotalWeight: WeightProjection,
    val missingWeightItemCount: Int,
    val ownedItemCount: Int,
    val unconfirmedOwnedItemCount: Int
)

data class TripEquipmentSnapshotSummaryReference(
    val snapshotId: String,
    val summary: TripEquipmentSummaryProjection
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RouteVersionReferenceProjection(
    val routeVersionId: String,
    val versionLabel: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RouteVersionDifferenceItem(
    val category: String,
    val changeType: String,
    val subject: String,
    val summary: String,
    val before: QualifiedValueProjection<String>? = null,
    val after: QualifiedValueProjection<String>? = null
)

data class RouteVersionDifferenceProjection(
    val minimumComparisonStatus: String,
    val differences: List<RouteVersionDifferenceItem>,
    val unavailableCategories: List<String>,
    val otherRelevantChangeStatus: String,
    val otherRelevantChanges: List<String>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripRouteVersionStatusProjection(
    val tripId: String,
    val tripRevision: String,
    val adoptedVersion: RouteVersionReferenceProjection,
    val currentPublicVersion: RouteVersionReferenceProjection? = null,
    val versionRelationship: String,
    val migrationEligible: Boolean,
    val difference: RouteVersionDifferenceProjection? = null
)

data class PersonalTripDetailProjection(
    val revision: String,
    val trip: PersonalTripProjection,
    val routeReference: TripRouteReference,
    val days: List<TripDayProjection>,
    val equipmentSnapshot: TripEquipmentSnapshotSummaryReference,
    val routeVersionStatus: TripRouteVersionStatusProjection
)

data class PersonalTripDaysProjection(
    val tripId: String,
    val revision: String,
    @BusinessDate val startDate: LocalDate,
    @BusinessDate val endDate: LocalDate,
    val totalDayCount: Int,
    val hikingDayCount: Int,
    val days: List<TripDayProjection>
)

data class TripDayWeatherItemProjection(
    val tripDayId: String,
    val dayNumber: Int,
    @BusinessDate val date: LocalDate,
    val primaryStage: String,
    val hikingDayNumber: Int?,
    val weather: TripDayWeatherProjection
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalTripWeatherProjection(
    val tripId: String,
    val revision: String,
    val routeName: String,
    @BusinessDate val startDate: LocalDate,
    @BusinessDate val endDate: LocalDate,
    val days: List<TripDayWeatherItemProjection>,
    val weatherOverview: QualifiedValueProjection<String>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripGenerationRouteProjection(
    val routeId: String,
    val currentPublicRouteVersionId: String,
    val routeType: String,
    val generationEligibility: RouteGenerationEligibility,
    val versionLabel: String? = null,
    val name: String? = null,
    val region: String? = null,
    val start: PublicRoutePlace? = null,
    val end: PublicRoutePlace? = null,
    val difficulty: String? = null,
    val distance: RouteMeters? = null,
    val estimatedDuration: RouteSeconds? = null
)

data class TripGenerationEquipmentListOption(
    val equipmentListId: String,
    val name: String,
    val summary: EquipmentListSummary
)

data class TripGenerationContextProjection(
    val route: TripGenerationRouteProjection,
    val personalEquipmentSummary: PersonalEquipmentSummary,
    val equipmentLists: List<TripGenerationEquipmentListOption>
)

data class TransportOptionProjection(
    val transportOptionId: String,
    val transferCount: QualifiedValueProjection<Any>,
    val estimatedArrivalAt: QualifiedValueProjection<Any>,
    val estimatedDuration: QualifiedValueProjection<Any>,
    val verificationItems: List<String>
)

data class TransportSelectionProjection(
    val selectionId: String,
    val options: List<TransportOptionProjection>
)

sealed interface GenerateTripResult {
    val resultType: String

        data class TripCreated(
        val trip: PersonalTripDetailProjection,
        override val resultType: String = "trip_created"
    ) : GenerateTripResult

        data class TransportSelectionRequired(
        val selection: TransportSelectionProjection,
        override val resultType: String = "transport_selection_required"
    ) : GenerateTripResult
}
