package org.example.trip.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.trip.repository.TripRepository
import org.example.trip.repository.TripParticipantRepository
import org.example.trip.repository.TripRouteAssociationRepository
import org.example.trip.model.Trip
import org.example.trip.model.TripRouteAssociation
import java.time.Instant
import java.util.*

/**
 * 行程服务实现类
 */
@Service
@Transactional
class TripServiceImpl(
    private val tripRepository: TripRepository,
    private val tripParticipantRepository: TripParticipantRepository,
    private val tripRouteAssociationRepository: TripRouteAssociationRepository
) : TripService {

    override fun getAllTrips(pageable: Pageable): Page<Trip> {
        return tripRepository.findAll(pageable)
    }

    override fun getTripById(id: String): Trip? {
        return tripRepository.findById(id).orElse(null)
    }

    override fun createTrip(trip: Trip): Trip {
        val tripToSave = trip.copy(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return tripRepository.save(tripToSave)
    }

    /**
     * 创建行程并写入行程-路线关联。
     *
     * 关联记录与行程记录处于同一事务（类级 `@Transactional`），任一步失败都会整体回滚，
     * 因此不会出现「行程已创建但没有任何路线」的残缺行程被读取到。
     */
    override fun createTrip(trip: Trip, routeIds: List<String>, primaryRouteId: String): Trip {
        val createdTrip = createTrip(trip.copy(primaryRouteId = primaryRouteId))

        val associations = routeIds.map { routeId ->
            TripRouteAssociation(
                tripId = createdTrip.id,
                routeId = routeId,
                isPrimary = routeId == primaryRouteId
            )
        }
        tripRouteAssociationRepository.saveAll(associations)

        return createdTrip
    }

    override fun updateTrip(id: String, trip: Trip): Trip? {
        return if (tripRepository.existsById(id)) {
            val updatedTrip = trip.copy(
                id = id,
                updatedAt = Instant.now()
            )
            tripRepository.save(updatedTrip)
        } else {
            null
        }
    }

    /**
     * 更新行程，并在主路线变更时同步关联表。
     *
     * 关联表是行程所含路线的权威来源，因此任何写入主路线的路径都必须同步维护它，
     * 否则一次编辑就会让权威数据与 Trip 行产生分歧。
     *
     * [newPrimaryRouteId] 为 null 表示本次更新未涉及主路线，关联保持原样。
     */
    override fun updateTrip(id: String, trip: Trip, newPrimaryRouteId: String?): Trip? {
        val updatedTrip = updateTrip(id, trip) ?: return null

        if (newPrimaryRouteId != null) {
            syncPrimaryRoute(id, newPrimaryRouteId)
        }

        return updatedTrip
    }

    /**
     * 将 [primaryRouteId] 标记为行程的主路线，并清除其余关联记录的主路线标记。
     *
     * 若该路线尚未与行程关联，则补建关联记录——保证「主路线必为行程路线之一」这条约束
     * 在更新路径上同样成立。
     */
    private fun syncPrimaryRoute(tripId: String, primaryRouteId: String) {
        val associations = tripRouteAssociationRepository.findByTripId(tripId)

        val existing = associations.find { it.routeId == primaryRouteId }
        if (existing == null) {
            tripRouteAssociationRepository.save(
                TripRouteAssociation(
                    tripId = tripId,
                    routeId = primaryRouteId,
                    isPrimary = true
                )
            )
        }

        val toDemote = associations.filter { it.routeId != primaryRouteId && it.isPrimary }
        val toPromote = existing?.takeIf { !it.isPrimary }

        if (toDemote.isEmpty() && toPromote == null) {
            return
        }

        toDemote.forEach { it.isPrimary = false }
        toPromote?.isPrimary = true

        tripRouteAssociationRepository.saveAll(toDemote + listOfNotNull(toPromote))
    }

    /**
     * 查询行程关联的路线标识，主路线排在首位。
     *
     * 无关联记录时返回空列表，由调用方（Controller）决定是否对历史数据回退推导。
     */
    override fun getRouteIds(tripId: String): List<String> {
        return tripRouteAssociationRepository.findByTripId(tripId)
            .sortedByDescending { it.isPrimary }
            .map { it.routeId }
    }

    override fun getRouteIdsByTripIds(tripIds: List<String>): Map<String, List<String>> {
        if (tripIds.isEmpty()) {
            return emptyMap()
        }
        return tripRouteAssociationRepository.findByTripIdIn(tripIds)
            .groupBy { it.tripId }
            .mapValues { (_, associations) ->
                associations.sortedByDescending { it.isPrimary }.map { it.routeId }
            }
    }

    override fun deleteTrip(id: String): Boolean {
        return if (tripRepository.existsById(id)) {
            tripRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    override fun searchTrips(
        keyword: String?,
        status: Int?,
        organizerId: String?,
        pageable: Pageable
    ): Page<Trip> {
        return tripRepository.searchTrips(
            organizerId = organizerId,
            status = status,
            startDateFrom = null,
            startDateTo = null,
            participantUserId = null,
            routeId = null,
            privacySetting = null,
            keyword = keyword,
            pageable = pageable
        )
    }

    override fun getUserTrips(userId: String, pageable: Pageable): Page<Trip> {
        return tripRepository.findAllUserTrips(userId, pageable)
    }

    override fun getUpcomingTrips(pageable: Pageable): Page<Trip> {
        return tripRepository.findUpcomingTrips(Instant.now(), 0, pageable) // 0 = PLANNING status
    }

    override fun getOngoingTrips(pageable: Pageable): Page<Trip> {
        return tripRepository.findByStatus(1, pageable) // 1 = ONGOING status
    }

    override fun getCompletedTrips(pageable: Pageable): Page<Trip> {
        return tripRepository.findByStatus(2, pageable) // 2 = COMPLETED status
    }

    override fun getPopularTrips(): List<Trip> {
        val page = tripRepository.findTop10PopularTrips(Pageable.ofSize(10))
        return page.content
    }

    override fun getRecentTrips(): List<Trip> {
        return tripRepository.findTop10ByOrderByCreatedAtDesc()
    }

    override fun getTripStatistics(): Map<String, Any> {
        return tripRepository.getTripStatistics()
    }

    override fun getTripsByOrganizer(organizerId: String, pageable: Pageable): Page<Trip> {
        return tripRepository.findByOrganizerId(organizerId, pageable)
    }

    override fun getTripsByStatus(status: Int, pageable: Pageable): Page<Trip> {
        return tripRepository.findByStatus(status, pageable)
    }

    override fun getTripsByParticipant(userId: String, pageable: Pageable): Page<Trip> {
        return tripRepository.findAllUserTrips(userId, pageable)
    }

    override fun updateTripStatus(id: String, status: Int): Trip? {
        val trip = tripRepository.findById(id).orElse(null)
        return if (trip != null) {
            val updatedTrip = trip.copy(
                status = status,
                updatedAt = Instant.now()
            )
            tripRepository.save(updatedTrip)
        } else {
            null
        }
    }

    override fun getPlannedTrips(pageable: Pageable): Page<Trip> {
        return tripRepository.findByStatus(0, pageable) // 0 = PLANNING status
    }
}