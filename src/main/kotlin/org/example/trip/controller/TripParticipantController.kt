package org.example.trip.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.example.trip.model.TripParticipant
import org.example.trip.service.TripParticipantService

/**
 * 行程参与者控制器
 */
@RestController
@RequestMapping("/api/trip-participants")
@CrossOrigin(origins = ["*"])
class TripParticipantController(
    private val tripParticipantService: TripParticipantService
) {

    /**
     * 添加行程参与者
     */
    @PostMapping
    fun addTripParticipant(
        @RequestParam tripId: String,
        @RequestParam userId: String,
        @RequestParam(defaultValue = "0") role: Int,
        @RequestParam(defaultValue = "0") status: Int
    ): ResponseEntity<TripParticipant> {
        return try {
            val participant = tripParticipantService.addParticipantToTrip(tripId, userId, role, status)
            ResponseEntity.status(HttpStatus.CREATED).body(participant)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 移除行程参与者
     */
    @DeleteMapping
    fun removeTripParticipant(
        @RequestParam tripId: String,
        @RequestParam userId: String
    ): ResponseEntity<Void> {
        val removed = tripParticipantService.removeParticipantFromTrip(tripId, userId)
        return if (removed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 获取行程的所有参与者
     */
    @GetMapping("/trip/{tripId}")
    fun getTripParticipants(
        @PathVariable tripId: String,
        pageable: Pageable
    ): ResponseEntity<Page<TripParticipant>> {
        val participants = tripParticipantService.getTripParticipants(tripId, pageable)
        return ResponseEntity.ok(participants)
    }

    /**
     * 获取用户参与的所有行程
     */
    @GetMapping("/user/{userId}")
    fun getUserTripParticipations(
        @PathVariable userId: String,
        pageable: Pageable
    ): ResponseEntity<Page<TripParticipant>> {
        val participations = tripParticipantService.getUserTripParticipations(userId, pageable)
        return ResponseEntity.ok(participations)
    }

    /**
     * 根据角色获取行程参与者
     */
    @GetMapping("/trip/{tripId}/role/{role}")
    fun getTripParticipantsByRole(
        @PathVariable tripId: String,
        @PathVariable role: Int
    ): ResponseEntity<List<TripParticipant>> {
        val participants = tripParticipantService.getTripParticipantsByRole(tripId, role)
        return ResponseEntity.ok(participants)
    }

    /**
     * 根据状态获取行程参与者
     */
    @GetMapping("/trip/{tripId}/status/{status}")
    fun getTripParticipantsByStatus(
        @PathVariable tripId: String,
        @PathVariable status: Int
    ): ResponseEntity<List<TripParticipant>> {
        val participants = tripParticipantService.getTripParticipantsByStatus(tripId, status)
        return ResponseEntity.ok(participants)
    }

    /**
     * 更新参与者角色
     */
    @PatchMapping("/role")
    fun updateParticipantRole(
        @RequestParam tripId: String,
        @RequestParam userId: String,
        @RequestParam role: Int
    ): ResponseEntity<TripParticipant> {
        val participant = tripParticipantService.updateParticipantRole(tripId, userId, role)
        return if (participant != null) {
            ResponseEntity.ok(participant)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 更新参与者状态
     */
    @PatchMapping("/status")
    fun updateParticipantStatus(
        @RequestParam tripId: String,
        @RequestParam userId: String,
        @RequestParam status: Int
    ): ResponseEntity<TripParticipant> {
        val participant = tripParticipantService.updateParticipantStatus(tripId, userId, status)
        return if (participant != null) {
            ResponseEntity.ok(participant)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 检查用户是否参与了行程
     */
    @GetMapping("/check")
    fun isUserParticipant(
        @RequestParam tripId: String,
        @RequestParam userId: String
    ): ResponseEntity<Map<String, Boolean>> {
        val isParticipant = tripParticipantService.isUserParticipant(tripId, userId)
        val response = mapOf("isParticipant" to isParticipant)
        return ResponseEntity.ok(response)
    }

    /**
     * 统计行程参与者数量
     */
    @GetMapping("/count/trip/{tripId}")
    fun countTripParticipants(@PathVariable tripId: String): ResponseEntity<Map<String, Long>> {
        val count = tripParticipantService.countTripParticipants(tripId)
        val response = mapOf("count" to count)
        return ResponseEntity.ok(response)
    }

    /**
     * 统计用户参与的行程数量
     */
    @GetMapping("/count/user/{userId}")
    fun countUserTripParticipations(@PathVariable userId: String): ResponseEntity<Map<String, Long>> {
        val count = tripParticipantService.countUserTripParticipations(userId)
        val response = mapOf("count" to count)
        return ResponseEntity.ok(response)
    }

    /**
     * 获取行程组织者
     */
    @GetMapping("/trip/{tripId}/organizers")
    fun getTripOrganizers(@PathVariable tripId: String): ResponseEntity<List<TripParticipant>> {
        val organizers = tripParticipantService.getTripOrganizers(tripId)
        return ResponseEntity.ok(organizers)
    }

    /**
     * 检查用户是否为行程组织者
     */
    @GetMapping("/check-organizer")
    fun isUserTripOrganizer(
        @RequestParam tripId: String,
        @RequestParam userId: String
    ): ResponseEntity<Map<String, Boolean>> {
        val isOrganizer = tripParticipantService.isUserTripOrganizer(tripId, userId)
        val response = mapOf("isOrganizer" to isOrganizer)
        return ResponseEntity.ok(response)
    }

    /**
     * 获取用户最近参与的行程
     */
    @GetMapping("/recent/user/{userId}")
    fun getRecentUserParticipations(
        @PathVariable userId: String,
        pageable: Pageable
    ): ResponseEntity<Page<TripParticipant>> {
        val participations = tripParticipantService.getRecentUserParticipations(userId, pageable)
        return ResponseEntity.ok(participations)
    }

    /**
     * 批量添加参与者
     */
    @PostMapping("/batch")
    fun addMultipleParticipants(
        @RequestParam tripId: String,
        @RequestBody userIds: List<String>,
        @RequestParam(defaultValue = "0") role: Int
    ): ResponseEntity<List<TripParticipant>> {
        return try {
            val participants = tripParticipantService.addMultipleParticipants(tripId, userIds, role)
            ResponseEntity.status(HttpStatus.CREATED).body(participants)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 批量移除参与者
     */
    @DeleteMapping("/batch")
    fun removeMultipleParticipants(
        @RequestParam tripId: String,
        @RequestBody userIds: List<String>
    ): ResponseEntity<Map<String, Long>> {
        val removedCount = tripParticipantService.removeMultipleParticipants(tripId, userIds)
        val response = mapOf("removedCount" to removedCount)
        return ResponseEntity.ok(response)
    }
}