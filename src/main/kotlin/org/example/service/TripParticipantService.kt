package org.example.service

import org.example.model.TripParticipant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 行程参与者服务接口
 */
interface TripParticipantService {

    // 基础操作
    fun addParticipantToTrip(tripId: String, userId: String, role: Int, status: Int = 0): TripParticipant
    fun removeParticipantFromTrip(tripId: String, userId: String): Boolean
    fun updateParticipantRole(tripId: String, userId: String, role: Int): TripParticipant?
    fun updateParticipantStatus(tripId: String, userId: String, status: Int): TripParticipant?

    // 查询操作
    fun getTripParticipants(tripId: String): List<TripParticipant>
    fun getTripParticipants(tripId: String, pageable: Pageable): Page<TripParticipant>
    fun getUserTripParticipations(userId: String): List<TripParticipant>
    fun getUserTripParticipations(userId: String, pageable: Pageable): Page<TripParticipant>
    fun getParticipant(tripId: String, userId: String): TripParticipant?

    // 角色和状态筛选
    fun getTripParticipantsByRole(tripId: String, role: Int): List<TripParticipant>
    fun getTripParticipantsByStatus(tripId: String, status: Int): List<TripParticipant>
    fun getUserParticipationsByStatus(userId: String, status: Int): List<TripParticipant>

    // 组织者相关
    fun getTripOrganizers(tripId: String): List<TripParticipant>
    fun isUserTripOrganizer(tripId: String, userId: String): Boolean

    // 统计功能
    fun countTripParticipants(tripId: String): Long
    fun countUserTripParticipations(userId: String): Long
    fun countParticipantsByRole(tripId: String, role: Int): Long
    fun countParticipantsByStatus(tripId: String, status: Int): Long

    // 检查功能
    fun isUserParticipant(tripId: String, userId: String): Boolean
    fun existsParticipant(tripId: String, userId: String): Boolean

    // 最近参与
    fun getRecentUserParticipations(userId: String, pageable: Pageable): Page<TripParticipant>

    // 批量操作
    fun addMultipleParticipants(tripId: String, userIds: List<String>, role: Int): List<TripParticipant>
    fun removeMultipleParticipants(tripId: String, userIds: List<String>): Long

    // 验证
    fun validateParticipation(tripId: String, userId: String, role: Int): Boolean
}