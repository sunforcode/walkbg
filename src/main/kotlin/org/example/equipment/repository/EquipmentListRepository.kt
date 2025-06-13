package org.example.equipment.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.example.equipment.model.EquipmentList
import java.time.Instant

@Repository
interface EquipmentListRepository : JpaRepository<EquipmentList, String> {
    
    fun findByCreatorId(creatorId: String, pageable: Pageable): Page<EquipmentList>
    
    fun findByCreatorIdAndStatus(creatorId: String, status: Int, pageable: Pageable): Page<EquipmentList>
    
    fun findByCreatorIdAndType(creatorId: String, type: Int, pageable: Pageable): Page<EquipmentList>
    
    @Query("SELECT el FROM EquipmentList el WHERE el.creatorId = :creatorId AND LOWER(el.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    fun searchByKeyword(@Param("creatorId") creatorId: String, @Param("keyword") keyword: String, pageable: Pageable): Page<EquipmentList>
    
    fun findByTripId(tripId: String): List<EquipmentList>
    
    fun findByTripId(tripId: String, pageable: Pageable): Page<EquipmentList>
    
    fun findByType(type: Int, pageable: Pageable): Page<EquipmentList>
    
    fun findByStatus(status: Int, pageable: Pageable): Page<EquipmentList>
    
    fun findByCreatorIdAndUpdatedAtAfter(creatorId: String, date: Instant, pageable: Pageable): Page<EquipmentList>

    fun findTop10ByCreatorIdOrderByCreatedAtDesc(creatorId: String): List<EquipmentList>

    fun findByCreatorIdOrderByUpdatedAtDesc(creatorId: String, pageable: Pageable): Page<EquipmentList>

    @Query("SELECT COUNT(el) FROM EquipmentList el WHERE el.creatorId = :creatorId")
    fun countByCreatorId(@Param("creatorId") creatorId: String): Long

    @Query("SELECT COUNT(el) FROM EquipmentList el WHERE el.tripId = :tripId")
    fun countByTripId(@Param("tripId") tripId: String): Long
}