package org.example.controller

import org.example.dto.TripDto
import org.example.dto.toDto
import org.example.model.Trip
import org.example.service.TripService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@RestController
@RequestMapping("/api/trips")
class TripController(private val tripService: TripService) {

    @GetMapping
    fun getAllTrips(): ResponseEntity<List<TripDto>> {
        return ResponseEntity.ok(tripService.getAllTrips().map { it.toDto() })
    }

    @GetMapping("/{id}")
    fun getTripById(@PathVariable id: String): ResponseEntity<TripDto> {
        val trip = tripService.getTripById(id)
        return if (trip != null) {
            ResponseEntity.ok(trip.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }
    @PostMapping
    fun createTrip(@RequestBody trip: Trip): ResponseEntity<TripDto> {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(trip).toDto())
    }

    @PostMapping("/sample")
    fun createSampleTrip(): ResponseEntity<TripDto> {
        val startDate = Instant.now().plus(30, ChronoUnit.DAYS)
        val endDate = startDate.plus(3, ChronoUnit.DAYS)

        val trip = Trip(
            id = UUID.randomUUID().toString(),
            name = "三日高山徒步",
            description = "三日高山徒步行程，包含湖泊和山脊露营",
            startDate = startDate,
            endDate = endDate,
            status = 0,
            participantCount = 3,
            organizerId = "user001",
            budget = 1500.0,
            notes = "需要准备防雨装备，山区天气多变",
            privacySetting = "public",
            coverUrl = "https://images.unsplash.com/photo-1551632811-561732d1e306",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val savedTrip = tripService.createTrip(trip)

        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrip.toDto())
    }

    @PutMapping("/{id}")
    fun updateTrip(@PathVariable id: String, @RequestBody trip: Trip): ResponseEntity<TripDto> {
        val updatedTrip = tripService.updateTrip(id, trip)
        return if (updatedTrip != null) {
            ResponseEntity.ok(updatedTrip.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteTrip(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = tripService.deleteTrip(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}