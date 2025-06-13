package org.example.trip.service

import org.example.trip.repository.TripParticipantRepository
import org.example.trip.model.TripParticipant
import org.example.trip.model.TripParticipantId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 行程参与者服务实现类
 */
@Service
@Transactional
class TripParticipantServiceImpl(
    private val tripParticipantRepository: TripParticipantRepository
) : TripParticipantService {

    // 基础操作
    override fun addParticipantToTrip(tripId: String, userId: String, role: Int, status: Int): TripParticipant {
        val participant = TripParticipant(
            tripId = tripId,
            userId = userId,
            role = role,
            status = status,
            joinedAt = Instant.now()
        )
        return tripParticipantRepository.save(participant)
    }

    override fun removeParticipantFromTrip(tripId: String, userId: String): Boolean {
        val participantId = TripParticipantId(tripId, userId)
        return if (tripParticipantRepository.existsById(participantId)) {
            tripParticipantRepository.deleteById(participantId)
            true
        } else {
            false
        }
    }

    override fun updateParticipantRole(tripId: String, userId: String, role: Int): TripParticipant? {
        val participant = tripParticipantRepository.findByTripIdAndUserId(tripId, userId)
        return if (participant != null) {
            val updated = participant.copy(role = role)
            tripParticipantRepository.save(updated)
        } else {
            null
        }
    }

    override fun updateParticipantStatus(tripId: String, userId: String, status: Int): TripParticipant? {
        val participant = tripParticipantRepository.findByTripIdAndUserId(tripId, userId)
        return if (participant != null) {
            val updated = participant.copy(status = status)
            tripParticipantRepository.save(updated)
        } else {
            null
        }
    }

    // 查询操作
    override fun getTripParticipants(tripId: String): List<TripParticipant> {
        return tripParticipantRepository.findByTripId(tripId)
    }

    override fun getTripParticipants(tripId: String, pageable: Pageable): Page<TripParticipant> {
        return tripParticipantRepository.findByTripId(tripId, pageable)
    }

    override fun getUserTripParticipations(userId: String): List<TripParticipant> {
        return tripParticipantRepository.findByUserId(userId)
    }

    override fun getUserTripParticipations(userId: String, pageable: Pageable): Page<TripParticipant> {
        return tripParticipantRepository.findByUserId(userId, pageable)
    }

    override fun getParticipant(tripId: String, userId: String): TripParticipant? {
        return tripParticipantRepository.findByTripIdAndUserId(tripId, userId)
    }

    // 角色和状态筛选
    override fun getTripParticipantsByRole(tripId: String, role: Int): List<TripParticipant> {
        return tripParticipantRepository.findByTripIdAndRole(tripId, role)
    }

    override fun getTripParticipantsByStatus(tripId: String, status: Int): List<TripParticipant> {
        return tripParticipantRepository.findByTripIdAndStatus(tripId, status)
    }

    override fun getUserParticipationsByStatus(userId: String, status: Int): List<TripParticipant> {
        return tripParticipantRepository.findByUserIdAndStatus(userId, status)
    }

    // 组织者相关
    override fun getTripOrganizers(tripId: String): List<TripParticipant> {
        return tripParticipantRepository.findTripOrganizers(tripId)
    }

    override fun isUserTripOrganizer(tripId: String, userId: String): Boolean {
        val participant = tripParticipantRepository.findByTripIdAndUserId(tripId, userId)
        return participant?.role == 1 // 假设1是组织者角色
    }

    // 统计功能
    override fun countTripParticipants(tripId: String): Long {
        return tripParticipantRepository.countByTripId(tripId)
    }

    override fun countUserTripParticipations(userId: String): Long {
        return tripParticipantRepository.countByUserId(userId)
    }

    override fun countParticipantsByRole(tripId: String, role: Int): Long {
        return tripParticipantRepository.findByTripIdAndRole(tripId, role).size.toLong()
    }

    override fun countParticipantsByStatus(tripId: String, status: Int): Long {
        return tripParticipantRepository.findByTripIdAndStatus(tripId, status).size.toLong()
    }

    // 检查功能
    override fun isUserParticipant(tripId: String, userId: String): Boolean {
        return tripParticipantRepository.existsByTripIdAndUserId(tripId, userId)
    }

    override fun existsParticipant(tripId: String, userId: String): Boolean {
        return tripParticipantRepository.existsByTripIdAndUserId(tripId, userId)
    }

    // 最近参与
    override fun getRecentUserParticipations(userId: String, pageable: Pageable): Page<TripParticipant> {
        return tripParticipantRepository.findRecentUserParticipations(userId, pageable)
    }

    // 批量操作
    override fun addMultipleParticipants(tripId: String, userIds: List<String>, role: Int): List<TripParticipant> {
        val participants = userIds.map { userId ->
            TripParticipant(
                tripId = tripId,
                userId = userId,
                role = role,
                status = 0,
                joinedAt = Instant.now()
            )
        }
        return tripParticipantRepository.saveAll(participants)
    }

    override fun removeMultipleParticipants(tripId: String, userIds: List<String>): Long {
        var removedCount = 0L
        userIds.forEach { userId ->
            if (removeParticipantFromTrip(tripId, userId)) {
                removedCount++
            }
        }
        return removedCount
    }

    // 验证
    override fun validateParticipation(tripId: String, userId: String, role: Int): Boolean {
        return tripId.isNotBlank() && 
               userId.isNotBlank() && 
               role in 0..2 && // 假设角色范围是0-2
               !existsParticipant(tripId, userId)
    }
}