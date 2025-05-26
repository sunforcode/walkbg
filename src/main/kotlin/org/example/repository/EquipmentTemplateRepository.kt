package org.example.repository

import org.example.model.EMEquipmentListType
import org.example.model.EMEquipmentTemplate
import org.example.model.EMSeasonSuitability
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EMEquipmentTemplateRepository : JpaRepository<EMEquipmentTemplate, String> {
    
    fun findByType(type: EMEquipmentListType, pageable: Pageable): Page<EMEquipmentTemplate>
    
    @Query("SELECT DISTINCT et FROM EMEquipmentTemplate et JOIN et.seasons s WHERE s.season = :season")
    fun findBySeason(season: EMSeasonSuitability, pageable: Pageable): Page<EMEquipmentTemplate>
    
    fun findByIsOfficial(isOfficial: Boolean, pageable: Pageable): Page<EMEquipmentTemplate>
    
    @Query("SELECT et FROM EMEquipmentTemplate et WHERE LOWER(et.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(et.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    fun searchByKeyword(keyword: String, pageable: Pageable): Page<EMEquipmentTemplate>
    
    fun findByCreatorId(creatorId: String, pageable: Pageable): Page<EMEquipmentTemplate>
    
    fun findTop10ByOrderByUsageCountDesc(): List<EMEquipmentTemplate>
    
    fun findTop10ByOrderByRatingDesc(): List<EMEquipmentTemplate>
    
    fun findTop10ByIsOfficialTrueOrderByUsageCountDesc(): List<EMEquipmentTemplate>
}