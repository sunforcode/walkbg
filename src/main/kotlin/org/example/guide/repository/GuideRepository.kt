package org.example.guide.repository

import org.example.guide.model.Guide
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 攻略仓库
 */
@Repository
interface GuideRepository : JpaRepository<Guide, String> {
    
    /**
     * 根据状态查找攻略
     */
    fun findByStatus(status: Int, pageable: Pageable): Page<Guide>
    
    /**
     * 根据标签查找攻略 (包含匹配)
     */
    fun findByTagsContaining(tag: String, pageable: Pageable): Page<Guide>
    
    /**
     * 根据作者ID查找攻略
     */
    fun findByAuthorId(authorId: String, pageable: Pageable): Page<Guide>
    
    /**
     * 根据标题模糊查询
     */
    fun findByTitleContainingIgnoreCase(title: String, pageable: Pageable): Page<Guide>
    
    /**
     * 多条件搜索攻略
     */
    @Query("""
        SELECT g FROM Guide g
        WHERE (:tag IS NULL OR g.tags LIKE CONCAT('%', :tag, '%'))
        AND (:status IS NULL OR g.status = :status)
        AND (:keyword IS NULL OR 
             LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(g.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    fun searchGuides(
        @Param("tag") tag: String?,
        @Param("status") status: Int?,
        @Param("keyword") keyword: String?,
        pageable: Pageable
    ): Page<Guide>
    
    /**
     * 查找热门攻略 (按浏览量和点赞数排序)
     */
    fun findTop10ByStatusOrderByViewCountDescLikeCountDesc(status: Int): List<Guide>
}
