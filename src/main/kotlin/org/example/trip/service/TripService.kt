package org.example.trip.service

import org.example.trip.model.Trip
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
     * 创建新行程并建立与路线的关联。
     *
     * 行程记录与关联记录写入同一事务，不会出现行程已创建但关联缺失的中间状态。
     *
     * @param routeIds 行程包含的路线集合，已由调用方归一化且非空
     * @param primaryRouteId 主路线标识，必为 [routeIds] 成员
     */
    fun createTrip(trip: Trip, routeIds: List<String>, primaryRouteId: String): Trip

    /**
     * 更新行程
     */
    fun updateTrip(id: String, trip: Trip): Trip?

    /**
     * 更新行程，并在主路线发生变更时同步维护关联记录。
     *
     * @param newPrimaryRouteId 本次请求显式指定的新主路线；null 表示本次更新不涉及主路线，关联保持不变
     */
    fun updateTrip(id: String, trip: Trip, newPrimaryRouteId: String?): Trip?
    
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

    /**
     * 获取计划中的行程
     */
    fun getPlannedTrips(pageable: Pageable): Page<Trip>

    /**
     * 查询行程关联的路线标识集合。
     *
     * 关联记录是行程所含路线的权威来源。主路线排在首位。
     * 对于无关联记录的历史行程返回空列表，由调用方决定是否回退推导。
     */
    fun getRouteIds(tripId: String): List<String>

    /**
     * 批量查询多个行程关联的路线标识，用于列表场景避免 N+1。
     *
     * 返回的 Map 仅包含存在关联记录的行程；无关联的行程不会出现在键集中。
     */
    fun getRouteIdsByTripIds(tripIds: List<String>): Map<String, List<String>>
}