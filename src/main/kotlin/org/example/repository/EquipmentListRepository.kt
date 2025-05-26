package org.example.repository

import org.example.model.EMEquipmentList
import org.example.model.EMEquipmentListStatus
import org.example.model.EMEquipmentListType
import org.example.model.EMSeasonSuitability
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EMEquipmentListRepository : JpaRepository<EMEquipmentList, String> {
    
    fun findByCreatorId(creatorId: String, pageable: Pageable): Page<EMEquipmentList>
    
    fun findByCreatorIdAndStatus(creatorId: String, status: EMEquipmentListStatus, pageable: Pageable): Page<EMEquipmentList>
    
    fun findByCreatorIdAndType(creatorId: String, type: EMEquipmentListType, pageable: Pageable): Page<EMEquipmentList>
    
    @Query("SELECT DISTINCT el FROM EMEquipmentList el JOIN el.seasons s WHERE el.creatorId = :creatorId AND s.season = :season")
    fun findByCreatorIdAndSeason(creatorId: String, season: EMSeasonSuitability, pageable: Pageable): Page<EMEquipmentList>
    
    @Query("SELECT el FROM EMEquipmentList el WHERE el.creatorId = :creatorId AND (LOWER(el.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(el.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    fun searchByKeyword(creatorId: String, keyword: String, pageable: Pageable): Page<EMEquipmentList>
    
    fun findByRouteId(routeId: String): List<EMEquipmentList>
    
    fun findByTripId(tripId: String): List<EMEquipmentList>
    
    fun findByIsTemplateTrue(pageable: Pageable): Page<EMEquipmentList>
    
    fun findByIsOfficialTrue(pageable: Pageable): Page<EMEquipmentList>
    
    fun findByCreatorIdAndLastUsedAtAfter(creatorId: String, date: Instant, pageable: Pageable): Page<EMEquipmentList>
    
    fun findTop10ByCreatorIdOrderByCreatedAtDesc(creatorId: String): List<EMEquipmentList>
}