package org.example.service

import org.example.model.*
import org.example.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TripService(private val tripRepository: TripRepository) {

    fun getAllTrips(): List<Trip> = tripRepository.findAll()
    
    fun getTripById(id: String): Trip? = tripRepository.findById(id).orElse(null)
    
    fun getTripsByName(name: String): List<Trip> = tripRepository.findByName(name)
    
    fun getTripsByOrganizerId(organizerId: String): List<Trip> = tripRepository.findByOrganizerId(organizerId)
    
    fun getTripsByStatus(status: Int): List<Trip> = tripRepository.findByStatus(status)
    
    fun getTripsByStartDateAfter(date: Instant): List<Trip> = tripRepository.findByStartDateAfter(date)
    
    fun getTripsByStartDateBefore(date: Instant): List<Trip> = tripRepository.findByStartDateBefore(date)
    
    fun getTripsByStartDateBetween(startDate: Instant, endDate: Instant): List<Trip> = 
        tripRepository.findByStartDateBetween(startDate, endDate)
    
    fun getTripsByParticipantUserId(userId: String): List<Trip> = tripRepository.findByParticipantUserId(userId)
    
    fun getTripsByRouteId(routeId: String): List<Trip> = tripRepository.findByRouteId(routeId)
    
    fun getTripsByPrivacySetting(privacySetting: String): List<Trip> = tripRepository.findByPrivacySetting(privacySetting)
    
    fun getRecentTrips(): List<Trip> = tripRepository.findTop10ByOrderByCreatedAtDesc()
    
    @Transactional
    fun createTrip(trip: Trip): Trip = tripRepository.save(trip)
    
    @Transactional
    fun updateTrip(id: String, trip: Trip): Trip? {
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
    
    @Transactional
    fun deleteTrip(id: String): Boolean {
        return if (tripRepository.existsById(id)) {
            tripRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    @Transactional
    fun addRouteToTrip(tripId: String, route: Route, isPrimary: Boolean = false): Trip? {
        val trip = getTripById(tripId) ?: return null
        trip.addRoute(route, isPrimary)
        return tripRepository.save(trip)
    }
    
    @Transactional
    fun addParticipantToTrip(tripId: String, participant: Participant): Trip? {
        val trip = getTripById(tripId) ?: return null
        trip.addParticipant(participant)
        return tripRepository.save(trip)
    }
    
    @Transactional
    fun addItineraryItemToTrip(tripId: String, itineraryItem: TripItinerary): Trip? {
        val trip = getTripById(tripId) ?: return null
        trip.addItineraryItem(itineraryItem)
        return tripRepository.save(trip)
    }
    
    @Transactional
    fun addImageToTrip(tripId: String, imageUrl: String, isCover: Boolean = false): Trip? {
        val trip = getTripById(tripId) ?: return null
        trip.addImage(imageUrl, isCover)
        return tripRepository.save(trip)
    }
    
    @Transactional
    fun updateTripStatus(tripId: String, status: Int): Trip? {
        val trip = getTripById(tripId) ?: return null
        val updatedTrip = trip.copy(status = status)
        return tripRepository.save(updatedTrip)
    }
}