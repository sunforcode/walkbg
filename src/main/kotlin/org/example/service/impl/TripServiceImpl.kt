package org.example.service.impl

import org.example.model.Trip
import org.example.model.TripRouteAssociation
import org.example.repository.TripRepository
import org.example.repository.TripParticipantRepository
import org.example.repository.TripRouteAssociationRepository
import org.example.service.TripService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
}