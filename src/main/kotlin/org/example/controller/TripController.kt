package org.example.controller

import org.example.model.Trip
import org.example.service.TripService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 行程控制器
 */
@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = ["*"])
class TripController(
    private val tripService: TripService
) {

    /**
     * 获取所有行程（分页）
     */
    @GetMapping
    fun getAllTrips(pageable: Pageable): ResponseEntity<Page<Trip>> {
        val trips = tripService.getAllTrips(pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 根据ID获取行程
     */
    @GetMapping("/{id}")
    fun getTripById(@PathVariable id: String): ResponseEntity<Trip> {
        val trip = tripService.getTripById(id)
        return if (trip != null) {
            ResponseEntity.ok(trip)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 创建行程
     */
    @PostMapping
    fun createTrip(@RequestBody trip: Trip): ResponseEntity<Trip> {
        return try {
            val createdTrip = tripService.createTrip(trip)
            ResponseEntity.status(HttpStatus.CREATED).body(createdTrip)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * 更新行程
     */
    @PutMapping("/{id}")
    fun updateTrip(@PathVariable id: String, @RequestBody trip: Trip): ResponseEntity<Trip> {
        val updatedTrip = tripService.updateTrip(id, trip)
        return if (updatedTrip != null) {
            ResponseEntity.ok(updatedTrip)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除行程
     */
    @DeleteMapping("/{id}")
    fun deleteTrip(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = tripService.deleteTrip(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 搜索行程
     */
    @GetMapping("/search")
    fun searchTrips(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: Int?,
        @RequestParam(required = false) organizerId: String?,
        pageable: Pageable
    ): ResponseEntity<Page<Trip>> {
        val trips = tripService.searchTrips(keyword, status, organizerId, pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取用户的行程
     */
    @GetMapping("/user/{userId}")
    fun getUserTrips(
        @PathVariable userId: String,
        pageable: Pageable
    ): ResponseEntity<Page<Trip>> {
        val trips = tripService.getUserTrips(userId, pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取即将开始的行程
     */
    @GetMapping("/upcoming")
    fun getUpcomingTrips(pageable: Pageable): ResponseEntity<Page<Trip>> {
        val trips = tripService.getUpcomingTrips(pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取热门行程
     */
    @GetMapping("/popular")
    fun getPopularTrips(): ResponseEntity<List<Trip>> {
        val trips = tripService.getPopularTrips()
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取最近创建的行程
     */
    @GetMapping("/recent")
    fun getRecentTrips(): ResponseEntity<List<Trip>> {
        val trips = tripService.getRecentTrips()
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取正在进行的行程
     */
    @GetMapping("/ongoing")
    fun getOngoingTrips(pageable: Pageable): ResponseEntity<Page<Trip>> {
        val trips = tripService.getOngoingTrips(pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取已完成的行程
     */
    @GetMapping("/completed")
    fun getCompletedTrips(pageable: Pageable): ResponseEntity<Page<Trip>> {
        val trips = tripService.getCompletedTrips(pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 获取行程统计信息
     */
    @GetMapping("/statistics")
    fun getTripStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = tripService.getTripStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * 获取用户参与的行程
     */
    @GetMapping("/participant/{userId}")
    fun getTripsByParticipant(
        @PathVariable userId: String,
        pageable: Pageable
    ): ResponseEntity<Page<Trip>> {
        val trips = tripService.getTripsByParticipant(userId, pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 更新行程状态
     */
    @PatchMapping("/{id}/status")
    fun updateTripStatus(
        @PathVariable id: String,
        @RequestParam status: Int
    ): ResponseEntity<Trip> {
        val trip = tripService.updateTripStatus(id, status)
        return if (trip != null) {
            ResponseEntity.ok(trip)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 根据组织者获取行程
     */
    @GetMapping("/organizer/{organizerId}")
    fun getTripsByOrganizer(
        @PathVariable organizerId: String,
        pageable: Pageable
    ): ResponseEntity<Page<Trip>> {
        val trips = tripService.getTripsByOrganizer(organizerId, pageable)
        return ResponseEntity.ok(trips)
    }

    /**
     * 根据状态获取行程
     */
    @GetMapping("/status/{status}")
    fun getTripsByStatus(
        @PathVariable status: Int,
        pageable: Pageable
    ): ResponseEntity<Page<Trip>> {
        val trips = tripService.getTripsByStatus(status, pageable)
        return ResponseEntity.ok(trips)
    }
}