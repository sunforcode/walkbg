package org.example.repository

import org.example.model.Trip
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TripRepository : JpaRepository<Trip, String> {
    
    fun findByName(name: String): List<Trip>
    
    fun findByOrganizerId(organizerId: String): List<Trip>
    
    fun findByStatus(status: Int): List<Trip>
    
    fun findByStartDateAfter(date: Instant): List<Trip>
    
    fun findByStartDateBefore(date: Instant): List<Trip>
    
    fun findByStartDateBetween(startDate: Instant, endDate: Instant): List<Trip>
    
    @Query("SELECT t FROM Trip t JOIN t.participants p WHERE p.userId = :userId")
    fun findByParticipantUserId(userId: String): List<Trip>
    
    @Query("SELECT t FROM Trip t JOIN t.tripRoutes tr WHERE tr.route.id = :routeId")
    fun findByRouteId(routeId: String): List<Trip>
    
    fun findByPrivacySetting(privacySetting: String): List<Trip>
    
    fun findTop10ByOrderByCreatedAtDesc(): List<Trip>
}