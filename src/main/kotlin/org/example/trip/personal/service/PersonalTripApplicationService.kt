package org.example.trip.personal.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.example.common.util.IdGenerator
import org.example.equipment.dto.EquipmentListSummary
import org.example.equipment.dto.PersonalEquipmentSummary
import org.example.equipment.dto.WeightProjection
import org.example.equipment.model.PersonalEquipmentRecord
import org.example.equipment.repository.EquipmentListMemberRepository
import org.example.equipment.repository.EquipmentListOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentOwnershipRepository
import org.example.equipment.repository.PersonalEquipmentRecordRepository
import org.example.equipment.repository.UserEquipmentListRecordRepository
import org.example.equipment.service.PersonalEquipmentDomainService
import org.example.route.dto.PublicRouteGeoPosition
import org.example.route.dto.PublicRoutePlace
import org.example.route.dto.RouteGenerationEligibility
import org.example.route.dto.RouteMeters
import org.example.route.dto.RouteSeconds
import org.example.route.model.RouteVersion
import org.example.route.repository.PublicRouteCollectionRepository
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionEquipmentSuggestionRepository
import org.example.route.repository.RouteVersionImageRepository
import org.example.route.repository.RouteVersionPointRepository
import org.example.route.repository.RouteVersionPublicationOrderRepository
import org.example.route.repository.RouteVersionRepository
import org.example.route.repository.RouteVersionSegmentRepository
import org.example.route.service.RouteVersionSummaryPlaceResolver
import org.example.route.service.RouteVersionSummaryPlaces
import org.example.trip.personal.dto.CalendarDayProjection
import org.example.trip.personal.dto.CancelTripCommand
import org.example.trip.personal.dto.FrozenRouteBasisProjection
import org.example.trip.personal.dto.GenerateTripCommand
import org.example.trip.personal.dto.GenerateTripResult
import org.example.trip.personal.dto.ImportantNoticeProjection
import org.example.trip.personal.dto.InformationConfidenceProjection
import org.example.trip.personal.dto.MigrateTripCommand
import org.example.trip.personal.dto.MigrationTargetChangedDetails
import org.example.trip.personal.dto.PersonalTripCalendarProjection
import org.example.trip.personal.dto.PersonalTripCollectionProjection
import org.example.trip.personal.dto.PersonalTripDaysProjection
import org.example.trip.personal.dto.PersonalTripDetailProjection
import org.example.trip.personal.dto.PersonalTripFocusProjection
import org.example.trip.personal.dto.PersonalTripProjection
import org.example.trip.personal.dto.PersonalTripWeatherProjection
import org.example.trip.personal.dto.QualifiedValueProjection
import org.example.trip.personal.dto.RouteSectionSnapshotProjection
import org.example.trip.personal.dto.RouteVersionReferenceProjection
import org.example.trip.personal.dto.TransportOptionProjection
import org.example.trip.personal.dto.TransportSelectionProjection
import org.example.trip.personal.dto.TripActionProjection
import org.example.trip.personal.dto.TripDayProjection
import org.example.trip.personal.dto.TripDayWeatherItemProjection
import org.example.trip.personal.dto.TripDayWeatherProjection
import org.example.trip.personal.dto.TripEquipmentSnapshotSummaryReference
import org.example.trip.personal.dto.TripEquipmentSummaryProjection
import org.example.trip.personal.dto.TripGenerationContextProjection
import org.example.trip.personal.dto.TripGenerationEquipmentListOption
import org.example.trip.personal.dto.TripGenerationRouteProjection
import org.example.trip.personal.dto.TripPointProjection
import org.example.trip.personal.dto.TripRouteReference
import org.example.trip.personal.dto.TripRouteVersionStatusProjection
import org.example.trip.personal.dto.TripSummaryProjection
import org.example.trip.personal.model.PersonalTripDayRecord
import org.example.trip.personal.model.PersonalTripEquipmentItemDerivedFromSuggestion
import org.example.trip.personal.model.PersonalTripEquipmentItemRecord
import org.example.trip.personal.model.PersonalTripEquipmentSnapshotRecord
import org.example.trip.personal.model.PersonalTripIdempotencyRecord
import org.example.trip.personal.model.PersonalTripOwnership
import org.example.trip.personal.model.PersonalTripRecord
import org.example.trip.personal.model.TripFrozenRouteVersion
import org.example.trip.personal.model.TripTransportSelectionRecord
import org.example.trip.personal.repository.PersonalTripDayRepository
import org.example.trip.personal.repository.PersonalTripEquipmentItemDerivationRepository
import org.example.trip.personal.repository.PersonalTripEquipmentItemRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSnapshotRepository
import org.example.trip.personal.repository.PersonalTripEquipmentSuppressionRepository
import org.example.trip.personal.repository.PersonalTripIdempotencyRepository
import org.example.trip.personal.repository.PersonalTripOwnershipRepository
import org.example.trip.personal.repository.PersonalTripRepository
import org.example.trip.personal.repository.TripFrozenRouteVersionRepository
import org.example.trip.personal.repository.TripTransportSelectionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

@Service
class PersonalTripApplicationService(
    private val tripRepository: PersonalTripRepository,
    private val ownershipRepository: PersonalTripOwnershipRepository,
    private val versionRelationRepository: TripFrozenRouteVersionRepository,
    private val dayRepository: PersonalTripDayRepository,
    private val snapshotRepository: PersonalTripEquipmentSnapshotRepository,
    private val itemRepository: PersonalTripEquipmentItemRepository,
    private val derivationRepository: PersonalTripEquipmentItemDerivationRepository,
    private val suppressionRepository: PersonalTripEquipmentSuppressionRepository,
    private val idempotencyRepository: PersonalTripIdempotencyRepository,
    private val selectionRepository: TripTransportSelectionRepository,
    private val collectionRepository: PublicRouteCollectionRepository,
    private val currentVersionRepository: RouteCurrentPublicVersionRepository,
    private val versionRepository: RouteVersionRepository,
    private val publicationOrderRepository: RouteVersionPublicationOrderRepository,
    private val imageRepository: RouteVersionImageRepository,
    private val segmentRepository: RouteVersionSegmentRepository,
    private val pointRepository: RouteVersionPointRepository,
    private val summaryPlaceResolver: RouteVersionSummaryPlaceResolver,
    private val suggestionRepository: RouteVersionEquipmentSuggestionRepository,
    private val listRepository: UserEquipmentListRecordRepository,
    private val listOwnershipRepository: EquipmentListOwnershipRepository,
    private val memberRepository: EquipmentListMemberRepository,
    private val equipmentRepository: PersonalEquipmentRecordRepository,
    private val equipmentOwnershipRepository: PersonalEquipmentOwnershipRepository,
    private val equipmentDomainService: PersonalEquipmentDomainService,
    private val domainService: PersonalTripDomainService,
    private val differenceService: RouteVersionDifferenceService,
    private val migrationEquipmentMerger: TripMigrationEquipmentMerger,
    private val planner: TripGenerationPlanner,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC()
) {
    @Transactional(readOnly = true)
    fun focus(accountId: String): PersonalTripFocusProjection {
        val records = ownedTrips(accountId)
        return PersonalTripFocusProjection(domainService.focus(records)?.let(::summary))
    }

    @Transactional(readOnly = true)
    fun collection(accountId: String): PersonalTripCollectionProjection {
        val grouped = domainService.groupAndOrder(ownedTrips(accountId))
        return PersonalTripCollectionProjection(
            currentTrips = grouped.current.map(::summary),
            historicalTrips = grouped.historical.map(::summary)
        )
    }

    @Transactional(readOnly = true)
    fun calendar(accountId: String): PersonalTripCalendarProjection = domainService.calendar(ownedTrips(accountId))

    @Transactional(readOnly = true)
    fun generationContext(accountId: String, routeId: String): TripGenerationContextProjection {
        val routeVersion = requireCurrentPublicRoute(routeId)
        val routeType = requireRouteType(routeVersion, ::personalTripReadFailure)
        val summaryPlaces = summaryPlaces(routeVersion)
        val ownedEquipment = ownedEquipment(accountId)
        val listOwnerships = listOwnershipRepository.findByAccountIdOrderByEquipmentListIdAsc(accountId)
        return TripGenerationContextProjection(
            route = TripGenerationRouteProjection(
                routeId = routeId,
                currentPublicRouteVersionId = routeVersion.id,
                routeType = routeType,
                generationEligibility = eligibility(routeVersion, summaryPlaces),
                versionLabel = routeVersion.versionLabel.nonBlankOrNull(),
                name = routeVersion.name.nonBlankOrNull(),
                region = routeVersion.region.nonBlankOrNull(),
                start = summaryPlaces.start,
                end = summaryPlaces.end,
                difficulty = routeVersion.difficulty.nonBlankOrNull(),
                distance = routeVersion.distanceMeters?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let(::RouteMeters),
                estimatedDuration = routeVersion.estimatedDurationSeconds?.takeIf { it >= 0 }?.toDouble()?.let(::RouteSeconds)
            ),
            personalEquipmentSummary = PersonalEquipmentSummary(
                equipmentItemCount = ownedEquipment.size,
                equipmentListCount = listOwnerships.size,
                knownTotalWeight = WeightProjection(
                    ownedEquipment.sumOf { (it.unitWeightGrams ?: 0L) * it.ownedQuantity.toLong() }
                ),
                missingWeightItemCount = ownedEquipment.count { it.unitWeightGrams == null }
            ),
            equipmentLists = listOwnerships.map { ownership ->
                val list = listRepository.findById(ownership.equipmentListId)
                    .orElseThrow(::personalTripReadFailure)
                val members = memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc(list.id)
                val equipment = members.map { member ->
                    equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId(
                        member.personalEquipmentId,
                        accountId
                    ) ?: throw personalTripReadFailure()
                    member to equipmentRepository.findById(member.personalEquipmentId)
                        .orElseThrow(::personalTripReadFailure)
                }
                TripGenerationEquipmentListOption(
                    equipmentListId = list.id,
                    name = list.name,
                    summary = EquipmentListSummary(
                        itemCount = equipment.size,
                        knownTotalWeight = WeightProjection(
                            equipment.sumOf { (member, item) ->
                                (item.unitWeightGrams ?: 0L) * member.quantity.toLong()
                            }
                        ),
                        missingWeightItemCount = equipment.count { (_, item) -> item.unitWeightGrams == null }
                    )
                )
            }
        )
    }

    @Transactional
    fun generate(accountId: String, idempotencyKey: String, command: GenerateTripCommand): GenerateTripResult {
        requireNonBlank(idempotencyKey, "Idempotency-Key")
        val requestHash = requestHash(command)
        replayGenerate(accountId, idempotencyKey, requestHash)?.let { return it }
        claimIdempotency(accountId, "generate_trip", idempotencyKey, requestHash)
            ?.let { return replayGenerate(it, requestHash) }
        requireNonBlank(command.routeId, "route_id")
        requireNonBlank(command.routeVersionId, "route_version_id")
        requireNonBlank(command.departureCity, "departure_city")
        if (command.startDate.isBefore(domainService.businessDate())) {
            throw ApiContractException.unprocessable("trip_start_date_invalid", "启程日期不能早于当前业务日期")
        }

        val routeVersion = requireCurrentPublicRoute(command.routeId)
        if (routeVersion.id != command.routeVersionId) {
            throw ApiContractException.conflict("route_version_conflict", "路线版本已不是当前公开版本")
        }
        requireRouteType(routeVersion, ::tripGenerationFailure)
        requireGenerationEligible(routeVersion, "trip_generation_failed")
        val selectedEquipment = selectedEquipment(accountId, command.equipmentListId)
        val transportOptionId = validateContinuation(accountId, command)
        val planningInput = TripPlanningInput(accountId, routeVersion, command, transportOptionId)

        return when (val decision = planner.plan(planningInput)) {
            TripPlanningDecision.Ready -> {
                val result = GenerateTripResult.TripCreated(createTrip(accountId, routeVersion, command, selectedEquipment))
                saveIdempotency(accountId, "generate_trip", idempotencyKey, requestHash, result.resultType, result)
                result
            }

            is TripPlanningDecision.SelectionRequired -> {
                if (decision.options.size < 2) throw tripGenerationFailure()
                val baseHash = selectionContextHash(command)
                val selection = TripTransportSelectionRecord(
                    selectionId = decision.selectionId,
                    accountId = accountId,
                    requestHash = baseHash,
                    contextJson = objectMapper.writeValueAsString(command.copy(transportSelection = null)),
                    optionsJson = objectMapper.writeValueAsString(decision.options),
                    createdAt = clock.instant()
                )
                selectionRepository.save(selection)
                val result = GenerateTripResult.TransportSelectionRequired(
                    TransportSelectionProjection(decision.selectionId, decision.options)
                )
                saveIdempotency(accountId, "generate_trip", idempotencyKey, requestHash, result.resultType, result)
                result
            }
        }
    }

    @Transactional(readOnly = true)
    fun detail(accountId: String, tripId: String): PersonalTripDetailProjection = detail(requireOwnedTrip(accountId, tripId))

    @Transactional(readOnly = true)
    fun days(accountId: String, tripId: String): PersonalTripDaysProjection {
        val detail = detail(requireOwnedTrip(accountId, tripId))
        return PersonalTripDaysProjection(
            tripId = detail.trip.identity,
            revision = detail.revision,
            startDate = detail.trip.startDate,
            endDate = detail.trip.endDate,
            totalDayCount = detail.trip.totalDayCount,
            hikingDayCount = detail.trip.hikingDayCount,
            days = detail.days
        )
    }

    @Transactional(readOnly = true)
    fun weather(accountId: String, tripId: String): PersonalTripWeatherProjection {
        val detail = detail(requireOwnedTrip(accountId, tripId))
        return PersonalTripWeatherProjection(
            tripId = detail.trip.identity,
            revision = detail.revision,
            routeName = detail.trip.frozenRouteBasis.routeName,
            startDate = detail.trip.startDate,
            endDate = detail.trip.endDate,
            weatherOverview = detail.trip.weatherOverview,
            days = detail.days.map {
                TripDayWeatherItemProjection(
                    tripDayId = it.identity,
                    dayNumber = it.dayNumber,
                    date = it.date,
                    primaryStage = it.primaryStage,
                    hikingDayNumber = it.hikingDayNumber,
                    weather = it.weather
                )
            }
        )
    }

    @Transactional
    fun cancel(
        accountId: String,
        tripId: String,
        idempotencyKey: String,
        command: CancelTripCommand
    ): PersonalTripDetailProjection {
        requireNonBlank(idempotencyKey, "Idempotency-Key")
        val operation = "cancel_trip:$tripId"
        val requestHash = requestHash(command)
        replayDetail(accountId, operation, idempotencyKey, requestHash)?.let { return it }
        claimIdempotency(accountId, operation, idempotencyKey, requestHash)
            ?.let { return replayDetail(it, requestHash) }
        if (!command.confirmed) {
            throw ApiContractException.unprocessable(
                "cancellation_confirmation_required",
                "取消行程需要明确确认"
            )
        }
        val trip = requireOwnedTripForUpdate(accountId, tripId)
        if (trip.revision != command.expectedRevision) {
            throw ApiContractException.conflict("concurrent_modification", "行程 revision 已变化")
        }
        if (domainService.status(trip) != "planned") {
            throw ApiContractException.conflict("trip_state_conflict", "只有规划中行程可以取消")
        }
        trip.lifecycleState = "cancelled"
        trip.revision = newRevision()
        trip.updatedAt = clock.instant()
        tripRepository.save(trip)
        val result = detail(trip)
        saveIdempotency(accountId, operation, idempotencyKey, requestHash, "personal_trip_detail", result)
        return result
    }

    @Transactional(readOnly = true)
    fun routeVersionStatus(accountId: String, tripId: String): TripRouteVersionStatusProjection =
        routeVersionStatus(requireOwnedTrip(accountId, tripId))

    @Transactional
    fun migrate(
        accountId: String,
        tripId: String,
        idempotencyKey: String,
        command: MigrateTripCommand
    ): PersonalTripDetailProjection {
        requireNonBlank(idempotencyKey, "Idempotency-Key")
        val operation = "migrate_trip:$tripId"
        val requestHash = requestHash(command)
        replayDetail(accountId, operation, idempotencyKey, requestHash)?.let { return it }
        claimIdempotency(accountId, operation, idempotencyKey, requestHash)
            ?.let { return replayDetail(it, requestHash) }
        val trip = requireOwnedTripForUpdate(accountId, tripId)
        if (trip.revision != command.expectedRevision) {
            throw ApiContractException.conflict("concurrent_modification", "行程 revision 已变化")
        }
        if (domainService.status(trip) != "planned") {
            throw ApiContractException.conflict("trip_state_conflict", "只有规划中行程可以迁移")
        }
        val relation = requireRouteRelation(trip.id)
        val adopted = versionRepository.findById(relation.routeVersionId).orElseThrow(::personalTripReadFailure)
        val current = currentVersionRepository.findByRouteIdForUpdate(adopted.routeId)
        if (current?.routeVersionId != command.targetRouteVersionId) {
            throw migrationTargetChanged(command.targetRouteVersionId, current?.routeVersionId)
        }
        val target = versionRepository.findById(command.targetRouteVersionId).orElse(null)
        if (target == null || target.routeId != adopted.routeId || !isExplicitlyNewer(adopted, target)) {
            throw migrationTargetChanged(command.targetRouteVersionId, current.routeVersionId)
        }

        val candidate = migrationCandidate(accountId, trip, target)

        derivationRepository.deleteByTripId(trip.id)
        itemRepository.deleteBySnapshotId(candidate.oldSnapshotId)
        snapshotRepository.deleteByTripId(trip.id)
        dayRepository.deleteByTripId(trip.id)
        derivationRepository.flush()
        itemRepository.flush()
        snapshotRepository.flush()
        dayRepository.flush()

        relation.routeVersionId = target.id
        versionRelationRepository.save(relation)
        trip.frozenRouteBasisJson = objectMapper.writeValueAsString(candidate.basis)
        trip.weatherOverviewJson = candidate.weatherOverviewJson
        trip.importantNoticesJson = candidate.importantNoticesJson
        trip.endDate = candidate.days.last().date
        trip.totalDayCount = candidate.days.size
        trip.hikingDayCount = candidate.days.count { it.hikingDayNumber != null }
        trip.revision = newRevision()
        trip.updatedAt = clock.instant()
        tripRepository.save(trip)
        dayRepository.saveAll(candidate.days)
        snapshotRepository.save(candidate.snapshot)
        itemRepository.saveAll(candidate.items)
        derivationRepository.saveAll(candidate.derivations)

        val result = detail(
            trip = trip,
            relation = relation,
            dayRecords = candidate.days,
            snapshot = candidate.snapshot,
            equipmentItems = candidate.items
        )
        saveIdempotency(accountId, operation, idempotencyKey, requestHash, "personal_trip_detail", result)
        return result
    }

    private fun migrationCandidate(
        accountId: String,
        trip: PersonalTripRecord,
        target: RouteVersion
    ): TripMigrationCandidate = try {
        requireRouteType(target, ::migrationGenerationFailure)
        requireGenerationEligible(target, "migration_generation_failed")
        val basis = frozenBasis(target)
        val days = generatedDays(
            tripId = trip.id,
            departureCity = trip.departureCity,
            startDate = trip.startDate,
            basis = basis,
            points = tripPoints(target)
        )
        validateCandidateDays(trip.startDate, basis, days)

        val oldSnapshot = requireSnapshot(trip.id)
        val oldItems = itemRepository.findBySnapshotIdOrderByDisplayOrderAsc(oldSnapshot.id)
        val suggestions = suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc(target.id)
        if (suggestions.any { it.routeVersionId != target.id }) throw migrationGenerationFailure()
        val mergedItems = migrationEquipmentMerger.merge(
            routeId = target.routeId,
            targetSuggestions = suggestions,
            oldItems = oldItems,
            oldDerivations = derivationRepository.findByTripIdOrderByItemIdAsc(trip.id),
            suppressions = suppressionRepository.findByTripIdOrderByLogicalSuggestionIdAsc(trip.id),
            ownedQuantitiesByNormalizedName = ownedEquipmentQuantitiesByNormalizedName(accountId)
        )
        val snapshotId = IdGenerator.generateIdWithPrefix("tes")
        val items = mergedItems.mapIndexed { index, item ->
            PersonalTripEquipmentItemRecord(
                id = IdGenerator.generateIdWithPrefix("tei"),
                snapshotId = snapshotId,
                displayOrder = index + 1,
                name = item.name,
                normalizedName = item.normalizedName,
                quantity = item.quantity,
                unitWeightGrams = item.unitWeightGrams,
                source = item.source,
                ownershipStatus = item.ownershipStatus,
                note = item.note
            )
        }
        val derivations = mergedItems.zip(items).mapNotNull { (merged, item) ->
            merged.logicalSuggestionId?.let { logicalId ->
                PersonalTripEquipmentItemDerivedFromSuggestion(
                    itemId = item.id,
                    tripId = trip.id,
                    logicalSuggestionId = logicalId,
                    suggestionOccurrenceId = merged.suggestionOccurrenceId,
                    routeId = target.routeId
                )
            }
        }
        TripMigrationCandidate(
            oldSnapshotId = oldSnapshot.id,
            basis = basis,
            days = days,
            weatherOverviewJson = null,
            importantNoticesJson = null,
            snapshot = equipmentSnapshot(snapshotId, trip.id, items),
            items = items,
            derivations = derivations
        )
    } catch (exception: ApiContractException) {
        if (exception.code == "migration_generation_failed") throw exception
        throw migrationGenerationFailure()
    } catch (_: Exception) {
        throw migrationGenerationFailure()
    }

    private fun validateCandidateDays(
        startDate: java.time.LocalDate,
        basis: FrozenRouteBasisProjection,
        days: List<PersonalTripDayRecord>
    ) {
        val projections = days.map { parse<TripDayProjection>(it.contentJson) }
        if (projections.isEmpty() || projections.map { it.dayNumber } != (1..projections.size).toList() ||
            projections.first().date != startDate || days.map { it.date } != projections.map { it.date } ||
            projections.any { it.actions.isEmpty() } ||
            (basis.routeType == "one_day" &&
                (projections.size != 1 || projections.single().hikingDayNumber != 1))
        ) {
            throw migrationGenerationFailure()
        }
    }

    private fun ownedEquipmentQuantitiesByNormalizedName(accountId: String): Map<String, Int> =
        equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(accountId)
            .map { ownership ->
                val record = equipmentRepository.findById(ownership.personalEquipmentId)
                    .orElseThrow(::migrationGenerationFailure)
                val normalized = equipmentDomainService.normalizeEquipmentName(record.name).comparison
                if (normalized != ownership.normalizedName || record.ownedQuantity <= 0) {
                    throw migrationGenerationFailure()
                }
                normalized to record.ownedQuantity
            }
            .groupingBy { it.first }
            .fold(0) { total, item -> total + item.second }

    private fun createTrip(
        accountId: String,
        routeVersion: RouteVersion,
        command: GenerateTripCommand,
        selectedEquipment: List<SelectedEquipment>
    ): PersonalTripDetailProjection {
        val basis = frozenBasis(routeVersion)
        val now = clock.instant()
        val tripId = IdGenerator.generateIdWithPrefix("pt")
        val days = generatedDays(
            tripId = tripId,
            departureCity = command.departureCity.trim(),
            startDate = command.startDate,
            basis = basis,
            points = tripPoints(routeVersion)
        )
        val trip = PersonalTripRecord(
            id = tripId,
            name = "${basis.routeName} · ${command.startDate}",
            firstGeneratedAt = now,
            lifecycleState = "active",
            departureCity = command.departureCity.trim(),
            startDate = command.startDate,
            endDate = days.last().date,
            totalDayCount = days.size,
            hikingDayCount = days.count { it.hikingDayNumber != null },
            revision = newRevision(),
            frozenRouteBasisJson = objectMapper.writeValueAsString(basis),
            selectedTransportOptionId = command.transportSelection?.transportOptionId,
            updatedAt = now
        )
        val snapshotId = IdGenerator.generateIdWithPrefix("tes")
        val allOwnedEquipment = ownedEquipmentByNormalizedName(accountId)
        val initialEquipment = initialEquipment(routeVersion, selectedEquipment)
        val itemRecords = initialEquipment.mapIndexed { index, item ->
            PersonalTripEquipmentItemRecord(
                id = IdGenerator.generateIdWithPrefix("tei"),
                snapshotId = snapshotId,
                displayOrder = index + 1,
                name = item.name,
                normalizedName = item.normalizedName,
                quantity = item.quantity,
                unitWeightGrams = item.unitWeightGrams,
                source = item.source,
                ownershipStatus = ownershipStatus(allOwnedEquipment[item.normalizedName], item.quantity),
                note = item.note
            )
        }
        val derivations = initialEquipment.zip(itemRecords).mapNotNull { (source, item) ->
            source.logicalSuggestionId?.let { logicalSuggestionId ->
                PersonalTripEquipmentItemDerivedFromSuggestion(
                    itemId = item.id,
                    tripId = tripId,
                    logicalSuggestionId = logicalSuggestionId,
                    suggestionOccurrenceId = source.suggestionOccurrenceId,
                    routeId = routeVersion.routeId
                )
            }
        }
        val snapshot = equipmentSnapshot(snapshotId, tripId, itemRecords)

        tripRepository.save(trip)
        ownershipRepository.save(PersonalTripOwnership(tripId, accountId))
        versionRelationRepository.save(TripFrozenRouteVersion(tripId, routeVersion.id))
        dayRepository.saveAll(days)
        snapshotRepository.save(snapshot)
        itemRepository.saveAll(itemRecords)
        if (derivations.isNotEmpty()) derivationRepository.saveAll(derivations)
        return detail(trip, TripFrozenRouteVersion(tripId, routeVersion.id), days, snapshot, itemRecords)
    }

    private fun generatedDays(
        tripId: String,
        departureCity: String,
        startDate: java.time.LocalDate,
        basis: FrozenRouteBasisProjection,
        points: List<TripPointProjection>
    ): List<PersonalTripDayRecord> {
        val dayCount = generatedDayCount(basis)
        val pathSlices = splitPath(basis.mainTrackPath, dayCount)
        val distancePerDay = basis.distance?.let { RouteMeters(it.meters / dayCount) }
        val ascentPerDay = basis.ascent?.let { RouteMeters(it.meters / dayCount) }
        val durationPerDay = RouteSeconds(basis.estimatedDuration.seconds / dayCount)
        val departure = PublicRoutePlace(departureCity)

        return (1..dayCount).map { dayNumber ->
            val dayId = IdGenerator.generateIdWithPrefix("ptd")
            val unavailable = unavailableValue()
            val sectionPath = pathSlices[dayNumber - 1]
            val sectionStart = PublicRoutePlace(
                name = if (dayNumber == 1) basis.start.name else "${basis.routeName} D$dayNumber 起点",
                position = sectionPath.first()
            )
            val sectionEnd = PublicRoutePlace(
                name = if (dayNumber == dayCount) basis.end.name else "${basis.routeName} D$dayNumber 终点",
                position = sectionPath.last()
            )
            val routeSection = RouteSectionSnapshotProjection(
                name = "${basis.routeName} D$dayNumber",
                start = sectionStart,
                end = sectionEnd,
                path = sectionPath
            )
            val actions = buildList {
                var sequence = 1
                if (dayNumber == 1) {
                    add(transportAction(sequence++, departure, basis.start, unavailable.confidence))
                }
                add(
                    TripActionProjection(
                        sequence = sequence++,
                        actionType = "hike",
                        routeSectionSnapshot = routeSection,
                        start = QualifiedValueProjection(value = sectionStart),
                        end = QualifiedValueProjection(value = sectionEnd),
                        distance = distancePerDay?.let { QualifiedValueProjection(value = it) }
                            ?: QualifiedValueProjection(confidence = unavailable.confidence),
                        ascent = ascentPerDay?.let { QualifiedValueProjection(value = it) }
                            ?: QualifiedValueProjection(confidence = unavailable.confidence),
                        estimatedDuration = QualifiedValueProjection(value = durationPerDay)
                    )
                )
                if (dayNumber == dayCount) {
                    add(transportAction(sequence, basis.end, departure, unavailable.confidence))
                }
            }
            val projection = TripDayProjection(
                identity = dayId,
                dayNumber = dayNumber,
                date = startDate.plusDays((dayNumber - 1).toLong()),
                primaryStage = "徒步",
                hikingDayNumber = dayNumber,
                actions = actions,
                weather = TripDayWeatherProjection(
                    condition = unavailable,
                    temperatureRange = unavailableValue(),
                    precipitation = unavailableValue(),
                    wind = unavailableValue(),
                    placeContext = unavailableValue(),
                    routeSectionContext = QualifiedValueProjection(value = routeSection)
                ),
                points = points.takeIf { it.isNotEmpty() && dayNumber == 1 }
            )
            PersonalTripDayRecord(
                id = dayId,
                tripId = tripId,
                dayNumber = dayNumber,
                date = projection.date,
                primaryStage = projection.primaryStage,
                hikingDayNumber = dayNumber,
                contentJson = objectMapper.writeValueAsString(projection)
            )
        }
    }

    private fun generatedDayCount(basis: FrozenRouteBasisProjection): Int = when (basis.routeType) {
        "one_day" -> 1
        "multi_day" -> kotlin.math.ceil(basis.estimatedDuration.seconds / SECONDS_PER_DAY).toInt().takeIf { it > 0 }
            ?: throw tripGenerationFailure()
        else -> throw tripGenerationFailure()
    }

    private fun splitPath(path: List<PublicRouteGeoPosition>, dayCount: Int): List<List<PublicRouteGeoPosition>> {
        if (path.isEmpty()) throw tripGenerationFailure()
        if (path.size == 1) return List(dayCount) { path }
        return (0 until dayCount).map { index ->
            val startIndex = (index.toLong() * (path.size - 1) / dayCount).toInt()
            val endIndex = ((index + 1).toLong() * (path.size - 1) / dayCount).toInt()
            listOf(path[startIndex], path[endIndex])
        }
    }

    private fun transportAction(
        sequence: Int,
        origin: PublicRoutePlace,
        destination: PublicRoutePlace,
        confidence: InformationConfidenceProjection?
    ) = TripActionProjection(
        sequence = sequence,
        actionType = "long_distance_transport",
        origin = QualifiedValueProjection(value = origin, confidence = confidence),
        destination = QualifiedValueProjection(value = destination, confidence = confidence),
        mode = QualifiedValueProjection(confidence = confidence),
        keyTimes = QualifiedValueProjection(confidence = confidence),
        estimatedDuration = QualifiedValueProjection(confidence = confidence),
        transferNotes = QualifiedValueProjection(confidence = confidence)
    )

    private fun tripPoints(version: RouteVersion): List<TripPointProjection> =
        pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id).mapNotNull { point ->
            val category = when (point.pointKind) {
                "campsite" -> "campsite"
                "overnight_place" -> "overnight_place"
                "water_source" -> "water_source"
                "supply_point" -> "supply"
                "key", "interest", "safety_notice", "start", "end" -> "other"
                else -> return@mapNotNull null
            }
            val name = point.name.nonBlankOrNull() ?: return@mapNotNull null
            val referenceSystem = point.referenceSystem.nonBlankOrNull() ?: return@mapNotNull null
            if (!point.latitude.isFinite() || !point.longitude.isFinite() ||
                point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0
            ) {
                return@mapNotNull null
            }
            TripPointProjection(
                sequence = point.displayOrder,
                category = category,
                type = point.subCategory.nonBlankOrNull() ?: point.category.nonBlankOrNull() ?: point.pointKind,
                name = name,
                location = QualifiedValueProjection(
                    value = PublicRouteGeoPosition(point.latitude, point.longitude, referenceSystem)
                ),
                details = point.description.nonBlankOrNull()?.let { QualifiedValueProjection(value = it) }
            )
        }

    private fun initialEquipment(
        routeVersion: RouteVersion,
        selectedEquipment: List<SelectedEquipment>
    ): List<InitialEquipmentItem> {
        val logicalIds = mutableSetOf<String>()
        val normalizedNames = mutableSetOf<String>()
        val items = suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc(routeVersion.id)
            .sortedBy { it.displayOrder }
            .map { suggestion ->
                val normalized = equipmentDomainService.normalizeEquipmentName(suggestion.name)
                if (suggestion.routeId != routeVersion.routeId || suggestion.routeVersionId != routeVersion.id ||
                    suggestion.displayOrder < 1 || !logicalIds.add(suggestion.logicalSuggestionId) ||
                    suggestion.normalizedName != normalized.comparison || !normalizedNames.add(normalized.comparison) ||
                    suggestion.quantity <= 0 || suggestion.unitWeightGrams?.let { it <= 0 } == true ||
                    suggestion.note?.isBlank() == true || suggestion.level !in SUGGESTION_LEVELS
                ) {
                    throw tripGenerationFailure()
                }
                InitialEquipmentItem(
                    name = normalized.display,
                    normalizedName = normalized.comparison,
                    quantity = suggestion.quantity,
                    unitWeightGrams = suggestion.unitWeightGrams,
                    note = suggestion.note,
                    source = "system_suggestion",
                    logicalSuggestionId = suggestion.logicalSuggestionId,
                    suggestionOccurrenceId = suggestion.id
                )
            }
            .toMutableList()

        selectedEquipment.forEach { selected ->
            val normalized = equipmentDomainService.normalizeEquipmentName(selected.record.name)
            if (!normalizedNames.add(normalized.comparison)) throw tripGenerationFailure()
            items += InitialEquipmentItem(
                name = normalized.display,
                normalizedName = normalized.comparison,
                quantity = selected.quantity,
                unitWeightGrams = selected.record.unitWeightGrams,
                source = "user_added"
            )
        }
        return items
    }

    private fun selectedEquipment(accountId: String, equipmentListId: String?): List<SelectedEquipment> {
        if (equipmentListId == null) return emptyList()
        listOwnershipRepository.findByEquipmentListIdAndAccountId(equipmentListId, accountId)
            ?: throw ApiContractException(HttpStatus.NOT_FOUND, "equipment_list_not_found", "装备清单不存在")
        listRepository.findById(equipmentListId)
            .orElseThrow { ApiContractException(HttpStatus.NOT_FOUND, "equipment_list_not_found", "装备清单不存在") }
        return memberRepository.findByEquipmentListIdOrderByPersonalEquipmentIdAsc(equipmentListId).map { member ->
            equipmentOwnershipRepository.findByPersonalEquipmentIdAndAccountId(member.personalEquipmentId, accountId)
                ?: throw tripGenerationFailure()
            val record = equipmentRepository.findById(member.personalEquipmentId)
                .orElseThrow(::tripGenerationFailure)
            if (member.quantity <= 0 || record.ownedQuantity <= 0 ||
                (record.unitWeightGrams != null && record.unitWeightGrams!! <= 0)
            ) {
                throw tripGenerationFailure()
            }
            SelectedEquipment(record, member.quantity)
        }
    }

    private fun validateContinuation(accountId: String, command: GenerateTripCommand): String? {
        val continuation = command.transportSelection ?: return null
        requireNonBlank(continuation.selectionId, "selection_id")
        requireNonBlank(continuation.transportOptionId, "transport_option_id")
        val selection = selectionRepository.findById(continuation.selectionId).orElseThrow(::transportSelectionInvalid)
        if (selection.accountId != accountId) throw transportSelectionInvalid()
        if (selection.requestHash != "placeholder" && selection.requestHash != selectionContextHash(command)) {
            throw transportSelectionInvalid()
        }
        if (selection.optionsJson != "placeholder") {
            val options: List<TransportOptionProjection> = try {
                objectMapper.readValue(selection.optionsJson, object : TypeReference<List<TransportOptionProjection>>() {})
            } catch (_: Exception) {
                throw transportSelectionInvalid()
            }
            if (options.none { it.transportOptionId == continuation.transportOptionId }) {
                throw transportSelectionInvalid()
            }
        }
        return continuation.transportOptionId
    }

    private fun frozenBasis(version: RouteVersion): FrozenRouteBasisProjection {
        val path = parseMainTrack(version)
        val summaryPlaces = summaryPlaces(version)
        val start = summaryPlaces.start ?: throw tripGenerationFailure()
        val end = summaryPlaces.end ?: throw tripGenerationFailure()
        val name = version.name.nonBlankOrNull() ?: throw tripGenerationFailure()
        val region = version.region.nonBlankOrNull() ?: throw tripGenerationFailure()
        val duration = version.estimatedDurationSeconds?.takeIf { it > 0 }?.toDouble()?.let(::RouteSeconds)
            ?: throw tripGenerationFailure()
        val cover = imageRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)
            .firstOrNull { it.role == "cover" }?.mediaReference.nonBlankOrNull()
        return FrozenRouteBasisProjection(
            routeName = name,
            routeType = requireRouteType(version, ::tripGenerationFailure),
            region = region,
            start = start,
            end = end,
            estimatedDuration = duration,
            mainTrackPath = path,
            versionLabel = version.versionLabel.nonBlankOrNull(),
            cover = cover,
            direction = version.direction.nonBlankOrNull(),
            distance = version.distanceMeters?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let(::RouteMeters),
            ascent = version.ascentMeters?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let(::RouteMeters),
            descent = version.descentMeters?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let(::RouteMeters)
        )
    }

    private fun parseMainTrack(version: RouteVersion): List<PublicRouteGeoPosition> {
        if (version.mainTrackAvailability != "valid") throw tripGenerationFailure()
        val referenceSystem = version.mainTrackReferenceSystem.nonBlankOrNull() ?: throw tripGenerationFailure()
        val raw: List<List<Double?>> = try {
            objectMapper.readValue(version.mainTrackJson, object : TypeReference<List<List<Double?>>>() {})
        } catch (_: Exception) {
            throw tripGenerationFailure()
        }
        val path = raw.map { point ->
            if (point.size < 2) throw tripGenerationFailure()
            val latitude = point[0]
            val longitude = point[1]
            if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0
            ) {
                throw tripGenerationFailure()
            }
            PublicRouteGeoPosition(latitude, longitude, referenceSystem)
        }
        if (path.isEmpty()) throw tripGenerationFailure()
        return path
    }

    private fun eligibility(
        version: RouteVersion,
        summaryPlaces: RouteVersionSummaryPlaces = summaryPlaces(version)
    ): RouteGenerationEligibility {
        val reasons = buildList {
            if (version.name.nonBlankOrNull() == null) add("name")
            if (version.region.nonBlankOrNull() == null) add("region")
            if ((version.estimatedDurationSeconds ?: 0L) <= 0L) add("estimatedDuration")
            if (summaryPlaces.start == null) add("start")
            if (summaryPlaces.end == null) add("end")
            if (!hasValidMainTrack(version)) add("validMainTrack")
        }
        return if (reasons.isEmpty()) RouteGenerationEligibility(true)
        else RouteGenerationEligibility(false, reasons)
    }

    private fun summaryPlaces(version: RouteVersion): RouteVersionSummaryPlaces =
        summaryPlaceResolver.resolve(
            version,
            pointRepository.findByRouteVersionIdOrderByDisplayOrderAsc(version.id)
        )

    private fun requireRouteType(version: RouteVersion, failure: () -> ApiContractException): String =
        version.routeType?.takeIf { it == "one_day" || it == "multi_day" } ?: throw failure()

    private fun hasValidMainTrack(version: RouteVersion): Boolean = try {
        parseMainTrack(version)
        true
    } catch (_: ApiContractException) {
        false
    }

    private fun requireGenerationEligible(version: RouteVersion, failureCode: String) {
        val eligibility = eligibility(version)
        if (!eligibility.eligible) {
            throw ApiContractException.serviceUnavailable(failureCode, "路线资料无法形成完整行程")
        }
    }

    private fun detail(
        trip: PersonalTripRecord,
        relation: TripFrozenRouteVersion = requireRouteRelation(trip.id),
        dayRecords: List<PersonalTripDayRecord> = dayRepository.findByTripIdOrderByDayNumberAsc(trip.id),
        snapshot: PersonalTripEquipmentSnapshotRecord = requireSnapshot(trip.id),
        equipmentItems: List<PersonalTripEquipmentItemRecord> =
            itemRepository.findBySnapshotIdOrderByDisplayOrderAsc(snapshot.id)
    ): PersonalTripDetailProjection {
        val basis = parse<FrozenRouteBasisProjection>(trip.frozenRouteBasisJson)
        val days = dayRecords.map { parse<TripDayProjection>(it.contentJson) }
        validateAggregate(trip, days)
        return PersonalTripDetailProjection(
            revision = trip.revision,
            trip = PersonalTripProjection(
                identity = trip.id,
                name = trip.name,
                firstGeneratedAt = trip.firstGeneratedAt,
                status = domainService.status(trip),
                departureCity = trip.departureCity,
                startDate = trip.startDate,
                endDate = trip.endDate,
                totalDayCount = trip.totalDayCount,
                hikingDayCount = trip.hikingDayCount,
                frozenRouteBasis = basis,
                weatherOverview = trip.weatherOverviewJson?.let { parse<QualifiedValueProjection<String>>(it) },
                importantNotices = trip.importantNoticesJson?.let { parse<List<ImportantNoticeProjection>>(it) }
            ),
            routeReference = TripRouteReference(
                routeId = requireVersion(relation.routeVersionId).routeId,
                adoptedRouteVersionId = relation.routeVersionId
            ),
            days = days,
            equipmentSnapshot = snapshotReference(snapshot, equipmentItems),
            routeVersionStatus = routeVersionStatus(trip, relation)
        )
    }

    private fun summary(trip: PersonalTripRecord): TripSummaryProjection {
        val basis = parse<FrozenRouteBasisProjection>(trip.frozenRouteBasisJson)
        val relation = requireRouteRelation(trip.id)
        return TripSummaryProjection(
            tripId = trip.id,
            revision = trip.revision,
            name = trip.name,
            status = domainService.status(trip),
            routeReference = TripRouteReference(requireVersion(relation.routeVersionId).routeId, relation.routeVersionId),
            routeName = basis.routeName,
            routeType = basis.routeType,
            start = basis.start,
            end = basis.end,
            estimatedDuration = basis.estimatedDuration,
            startDate = trip.startDate,
            endDate = trip.endDate,
            totalDayCount = trip.totalDayCount,
            hikingDayCount = trip.hikingDayCount,
            versionLabel = basis.versionLabel,
            cover = basis.cover,
            direction = basis.direction,
            distance = basis.distance,
            ascent = basis.ascent
        )
    }

    private fun routeVersionStatus(
        trip: PersonalTripRecord,
        relation: TripFrozenRouteVersion = requireRouteRelation(trip.id)
    ): TripRouteVersionStatusProjection {
        val adopted = requireVersion(relation.routeVersionId)
        val currentRelation = currentVersionRepository.findById(adopted.routeId).orElse(null)
        val current = currentRelation?.let { versionRepository.findById(it.routeVersionId).orElse(null) }
            ?.takeIf { it.routeId == adopted.routeId }
        return when {
            current == null -> TripRouteVersionStatusProjection(
                tripId = trip.id,
                tripRevision = trip.revision,
                adoptedVersion = versionReference(adopted),
                versionRelationship = "current_public_version_unavailable",
                migrationEligible = false
            )

            current.id == adopted.id -> TripRouteVersionStatusProjection(
                tripId = trip.id,
                tripRevision = trip.revision,
                adoptedVersion = versionReference(adopted),
                currentPublicVersion = versionReference(current),
                versionRelationship = "current",
                migrationEligible = false
            )

            isExplicitlyNewer(adopted, current) -> TripRouteVersionStatusProjection(
                tripId = trip.id,
                tripRevision = trip.revision,
                adoptedVersion = versionReference(adopted),
                currentPublicVersion = versionReference(current),
                versionRelationship = "newer_public_version_available",
                migrationEligible = domainService.status(trip) == "planned",
                difference = differenceService.compare(
                    adopted,
                    current,
                    segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(adopted.id),
                    segmentRepository.findByRouteVersionIdOrderBySegmentOrderAsc(current.id),
                    suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc(adopted.id),
                    suggestionRepository.findByRouteVersionIdOrderByDisplayOrderAsc(current.id)
                )
            )

            else -> TripRouteVersionStatusProjection(
                tripId = trip.id,
                tripRevision = trip.revision,
                adoptedVersion = versionReference(adopted),
                versionRelationship = "current_public_version_unavailable",
                migrationEligible = false
            )
        }
    }

    private fun isExplicitlyNewer(adopted: RouteVersion, current: RouteVersion): Boolean {
        val adoptedPublication = publicationOrderRepository.findByRouteVersionId(adopted.id) ?: return false
        val currentPublication = publicationOrderRepository.findByRouteVersionId(current.id) ?: return false
        return adoptedPublication.routeId == adopted.routeId &&
            currentPublication.routeId == adopted.routeId &&
            adoptedPublication.publishedSequence < currentPublication.publishedSequence
    }

    private fun validateAggregate(trip: PersonalTripRecord, days: List<TripDayProjection>) {
        if (days.isEmpty() || days.map { it.dayNumber } != (1..days.size).toList() ||
            trip.totalDayCount != days.size || trip.startDate != days.first().date || trip.endDate != days.last().date ||
            trip.hikingDayCount != days.count { it.hikingDayNumber != null }
        ) {
            throw personalTripReadFailure()
        }
        val basis = parse<FrozenRouteBasisProjection>(trip.frozenRouteBasisJson)
        if (basis.routeType == "one_day" &&
            (days.size != 1 || days.single().dayNumber != 1 || days.single().hikingDayNumber != 1)
        ) {
            throw personalTripReadFailure()
        }
    }

    private fun snapshotReference(
        snapshot: PersonalTripEquipmentSnapshotRecord,
        items: List<PersonalTripEquipmentItemRecord>
    ) = TripEquipmentSnapshotSummaryReference(
        snapshotId = snapshot.id,
        summary = TripEquipmentSummaryProjection(
            itemCount = items.size,
            knownTotalWeight = WeightProjection(
                items.sumOf { (it.unitWeightGrams ?: 0L) * it.quantity.toLong() }
            ),
            missingWeightItemCount = items.count { it.unitWeightGrams == null },
            ownedItemCount = items.count { it.ownershipStatus == "owned" },
            unconfirmedOwnedItemCount = items.count { it.ownershipStatus == "unconfirmed_owned" }
        )
    )

    private fun ownedTrips(accountId: String): List<PersonalTripRecord> =
        ownershipRepository.findByAccountIdOrderByTripIdAsc(accountId).map { ownership ->
            tripRepository.findById(ownership.tripId).orElseThrow(::personalTripReadFailure)
        }

    private fun ownedEquipment(accountId: String): List<PersonalEquipmentRecord> =
        equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(accountId).map { ownership ->
            equipmentRepository.findById(ownership.personalEquipmentId).orElseThrow(::personalTripReadFailure)
        }

    private fun ownedEquipmentByNormalizedName(accountId: String): Map<String, PersonalEquipmentRecord> =
        equipmentOwnershipRepository.findByAccountIdOrderByPersonalEquipmentIdAsc(accountId).associate { ownership ->
            ownership.normalizedName to equipmentRepository.findById(ownership.personalEquipmentId)
                .orElseThrow(::personalTripReadFailure)
        }

    private fun requireOwnedTrip(accountId: String, tripId: String): PersonalTripRecord {
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw tripNotFound()
        return tripRepository.findById(tripId).orElseThrow(::tripNotFound)
    }

    private fun requireOwnedTripForUpdate(accountId: String, tripId: String): PersonalTripRecord {
        val trip = tripRepository.findByIdForUpdate(tripId) ?: throw tripNotFound()
        ownershipRepository.findByTripIdAndAccountId(tripId, accountId) ?: throw tripNotFound()
        return trip
    }

    private fun requireRouteRelation(tripId: String): TripFrozenRouteVersion =
        versionRelationRepository.findById(tripId).orElseThrow(::personalTripReadFailure)

    private fun requireSnapshot(tripId: String): PersonalTripEquipmentSnapshotRecord =
        snapshotRepository.findByTripId(tripId) ?: throw personalTripReadFailure()

    private fun requireVersion(versionId: String): RouteVersion =
        versionRepository.findById(versionId).orElseThrow(::personalTripReadFailure)

    private fun requireCurrentPublicRoute(routeId: String): RouteVersion {
        collectionRepository.findById(routeId).orElseThrow(::routeNotFound)
        val current = currentVersionRepository.findById(routeId).orElseThrow(::routeNotFound)
        val version = versionRepository.findById(current.routeVersionId).orElseThrow(::routeNotFound)
        if (version.routeId != routeId) throw routeNotFound()
        return version
    }

    private fun replayGenerate(accountId: String, key: String, hash: String): GenerateTripResult? {
        if (key.isBlank()) return null
        val record = idempotencyRepository.findByAccountIdAndOperationAndIdempotencyKey(accountId, "generate_trip", key)
            ?: return null
        return replayGenerate(record, hash)
    }

    private fun replayGenerate(record: PersonalTripIdempotencyRecord, hash: String): GenerateTripResult {
        ensureSameIdempotentRequest(record, hash)
        return when (record.responseType) {
            "trip_created" -> objectMapper.readValue(record.responseJson, GenerateTripResult.TripCreated::class.java)
            "transport_selection_required" -> objectMapper.readValue(
                record.responseJson,
                GenerateTripResult.TransportSelectionRequired::class.java
            )
            else -> throw personalTripReadFailure()
        }
    }

    private fun claimIdempotency(
        accountId: String,
        operation: String,
        key: String,
        requestHash: String
    ): PersonalTripIdempotencyRecord? {
        val inserted = idempotencyRepository.insertReservation(
            id = IdGenerator.generateIdWithPrefix("pti"),
            accountId = accountId,
            operation = operation,
            idempotencyKey = key,
            requestHash = requestHash,
            responseType = "pending",
            responseJson = "{}"
        )
        if (inserted == 1) return null
        return idempotencyRepository.findClaimForUpdate(accountId, operation, key)
            ?: throw personalTripReadFailure()
    }

    private fun replayDetail(accountId: String, operation: String, key: String, hash: String): PersonalTripDetailProjection? {
        if (key.isBlank()) return null
        val record = idempotencyRepository.findByAccountIdAndOperationAndIdempotencyKey(accountId, operation, key)
            ?: return null
        return replayDetail(record, hash)
    }

    private fun replayDetail(record: PersonalTripIdempotencyRecord, hash: String): PersonalTripDetailProjection {
        ensureSameIdempotentRequest(record, hash)
        if (record.responseType != "personal_trip_detail") throw personalTripReadFailure()
        return objectMapper.readValue(record.responseJson, PersonalTripDetailProjection::class.java)
    }

    private fun ensureSameIdempotentRequest(record: PersonalTripIdempotencyRecord, requestHash: String) {
        if (record.requestHash != requestHash) {
            throw ApiContractException.conflict("idempotency_conflict", "幂等键已用于不同请求")
        }
    }

    private fun saveIdempotency(
        accountId: String,
        operation: String,
        key: String,
        requestHash: String,
        responseType: String,
        response: Any
    ) {
        val responseJson = objectMapper.writeValueAsString(response)
        val completed = idempotencyRepository.completeReservation(
            accountId = accountId,
            operation = operation,
            idempotencyKey = key,
            requestHash = requestHash,
            responseType = responseType,
            responseJson = responseJson
        )
        if (completed == 0) {
            idempotencyRepository.save(
                PersonalTripIdempotencyRecord(
                    id = IdGenerator.generateIdWithPrefix("pti"),
                    accountId = accountId,
                    operation = operation,
                    idempotencyKey = key,
                    requestHash = requestHash,
                    responseType = responseType,
                    responseJson = responseJson,
                    createdAt = clock.instant()
                )
            )
        }
    }

    private fun requestHash(value: Any): String = sha256(objectMapper.writeValueAsBytes(value))

    private fun selectionContextHash(command: GenerateTripCommand): String =
        requestHash(command.copy(transportSelection = null))

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private inline fun <reified T> parse(json: String): T = try {
        objectMapper.readValue(json, object : TypeReference<T>() {})
    } catch (_: Exception) {
        throw personalTripReadFailure()
    }

    private fun versionReference(version: RouteVersion) =
        RouteVersionReferenceProjection(version.id, version.versionLabel.nonBlankOrNull())

    private fun newRevision(): String = IdGenerator.generateIdWithPrefix("rev")

    private fun ownershipStatus(record: PersonalEquipmentRecord?, requiredQuantity: Int): String =
        if ((record?.ownedQuantity ?: 0) >= requiredQuantity) "owned" else "unconfirmed_owned"

    private fun equipmentSnapshot(
        snapshotId: String,
        tripId: String,
        items: List<PersonalTripEquipmentItemRecord>
    ) = PersonalTripEquipmentSnapshotRecord(
        id = snapshotId,
        tripId = tripId,
        itemCount = items.size,
        knownTotalWeightGrams = items.sumOf { (it.unitWeightGrams ?: 0L) * it.quantity.toLong() },
        missingWeightItemCount = items.count { it.unitWeightGrams == null },
        ownedItemCount = items.count { it.ownershipStatus == "owned" },
        unconfirmedOwnedItemCount = items.count { it.ownershipStatus == "unconfirmed_owned" }
    )

    private fun unavailableValue(): QualifiedValueProjection<Any> = QualifiedValueProjection(
        confidence = InformationConfidenceProjection("unavailable", "dynamic_external_information")
    )

    private fun requireNonBlank(value: String, field: String) {
        if (value.isBlank()) throw ApiContractException.invalidRequest("$field 不能为空")
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private data class SelectedEquipment(val record: PersonalEquipmentRecord, val quantity: Int)

    private data class InitialEquipmentItem(
        val name: String,
        val normalizedName: String,
        val quantity: Int,
        val unitWeightGrams: Long?,
        val note: String? = null,
        val source: String,
        val logicalSuggestionId: String? = null,
        val suggestionOccurrenceId: String? = null
    )

    private data class TripMigrationCandidate(
        val oldSnapshotId: String,
        val basis: FrozenRouteBasisProjection,
        val days: List<PersonalTripDayRecord>,
        val weatherOverviewJson: String?,
        val importantNoticesJson: String?,
        val snapshot: PersonalTripEquipmentSnapshotRecord,
        val items: List<PersonalTripEquipmentItemRecord>,
        val derivations: List<PersonalTripEquipmentItemDerivedFromSuggestion>
    )

    private fun migrationTargetChanged(requestedTargetId: String, currentTargetId: String?) =
        ApiContractException(
            HttpStatus.CONFLICT,
            "migration_target_changed",
            "迁移目标已不是当前公开版本",
            details = MigrationTargetChangedDetails(requestedTargetId, currentTargetId)
        )

    private fun routeNotFound() = ApiContractException(HttpStatus.NOT_FOUND, "route_not_found", "路线不存在")
    private fun tripNotFound() = ApiContractException(HttpStatus.NOT_FOUND, "trip_not_found", "行程不存在")
    private fun personalTripReadFailure() =
        ApiContractException.serviceUnavailable("personal_trip_read_failed", "个人行程暂时无法读取")

    private fun tripGenerationFailure() =
        ApiContractException.serviceUnavailable("trip_generation_failed", "无法形成完整个人行程")

    private fun migrationGenerationFailure() =
        ApiContractException.serviceUnavailable("migration_generation_failed", "无法形成完整迁移行程")

    private fun transportSelectionInvalid() =
        ApiContractException.conflict("transport_selection_invalid", "交通候选上下文或选择无效")

    private companion object {
        const val SECONDS_PER_DAY = 86_400.0
        val SUGGESTION_LEVELS = setOf("required", "recommended")
    }
}
