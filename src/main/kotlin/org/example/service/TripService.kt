package org.example.service

import org.example.model.Trip
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 行程服务接口
 */
interface TripService {

    /**
     * 获取所有行程（分页）
     */
    fun getAllTrips(pageable: Pageable): Page<Trip>
    
    /**
     * 根据ID获取行程详情
     */
    fun getTripById(id: String): Trip?
    
    /**
     * 创建新行程
     */
    fun createTrip(trip: Trip): Trip
    
    /**
     * 更新行程
     */
    fun updateTrip(id: String, trip: Trip): Trip?
    
    /**
     * 删除行程
     */
    fun deleteTrip(id: String): Boolean

    /**
     * 搜索行程
     */
    fun searchTrips(
        keyword: String?,
        status: Int?,
        organizerId: String?,
        pageable: Pageable
    ): Page<Trip>

    /**
     * 获取用户的所有行程
     */
    fun getUserTrips(userId: String, pageable: Pageable): Page<Trip>

    /**
     * 获取即将开始的行程
     */
    fun getUpcomingTrips(pageable: Pageable): Page<Trip>

    /**
     * 获取正在进行的行程
     */
    fun getOngoingTrips(pageable: Pageable): Page<Trip>

    /**
     * 获取已完成的行程
     */
    fun getCompletedTrips(pageable: Pageable): Page<Trip>

    /**
     * 获取热门行程
     */
    fun getPopularTrips(): List<Trip>

    /**
     * 获取最近创建的行程
     */
    fun getRecentTrips(): List<Trip>

    /**
     * 获取行程统计信息
     */
    fun getTripStatistics(): Map<String, Any>

    /**
     * 根据组织者获取行程
     */
    fun getTripsByOrganizer(organizerId: String, pageable: Pageable): Page<Trip>

    /**
     * 根据状态获取行程
     */
    fun getTripsByStatus(status: Int, pageable: Pageable): Page<Trip>

    /**
     * 获取用户参与的行程
     */
    fun getTripsByParticipant(userId: String, pageable: Pageable): Page<Trip>

    /**
     * 更新行程状态
     */
    fun updateTripStatus(id: String, status: Int): Trip?
}