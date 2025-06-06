package org.example.repository

import org.example.model.EquipmentTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EquipmentTemplateRepository : JpaRepository<EquipmentTemplate, String> {
    
    /**
     * 按创建者查询
     */
    fun findByCreatorId(creatorId: String, pageable: Pageable): Page<EquipmentTemplate>
    
    /**
     * 查询官方模板
     */
    fun findByIsOfficialTrue(pageable: Pageable): Page<EquipmentTemplate>
    
    fun findByIsOfficialFalse(pageable: Pageable): Page<EquipmentTemplate>

    fun findByCreatorIdAndIsOfficialFalse(creatorId: String, pageable: Pageable): Page<EquipmentTemplate>

    /**
     * 按类型查询
     */
    fun findByType(type: Int, pageable: Pageable): Page<EquipmentTemplate>

    /**
     * 按分类查询
     */
    fun findByCategory(category: Int, pageable: Pageable): Page<EquipmentTemplate>

    /**
     * 关键词搜索
     */
    @Query("SELECT et FROM EquipmentTemplate et WHERE LOWER(et.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    fun searchByName(@Param("keyword") keyword: String, pageable: Pageable): Page<EquipmentTemplate>
    
    /**
     * 多条件查询
     */
    @Query("""
        SELECT et FROM EquipmentTemplate et
        WHERE (:keyword IS NULL OR LOWER(et.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:category IS NULL OR et.category = :category)
        AND (:type IS NULL OR et.type = :type)
        AND (:isOfficial IS NULL OR et.isOfficial = :isOfficial)
    """)
    fun searchTemplates(
        @Param("keyword") keyword: String?,
        @Param("category") category: Int?,
        @Param("type") type: Int?,
        @Param("isOfficial") isOfficial: Boolean?,
        pageable: Pageable
    ): Page<EquipmentTemplate>

    /**
     * 按使用次数排序
     */
    fun findTop10ByOrderByUsageCountDesc(): List<EquipmentTemplate>

    /**
     * 按评分排序
     */
    fun findTop10ByOrderByRatingDesc(): List<EquipmentTemplate>

    /**
     * 官方热门模板
     */
    fun findTop10ByOrderByCreatedAtDesc(): List<EquipmentTemplate>

    fun findByCategoryAndType(category: Int, type: Int, pageable: Pageable): Page<EquipmentTemplate>
}