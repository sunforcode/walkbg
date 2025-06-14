package org.example.trip.repository

import org.example.trip.model.TripContact
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TripContactRepository : JpaRepository<TripContact, String> {
    
    /**
     * 根据行程ID查找联系人关联
     */
    fun findByTripId(tripId: String, pageable: Pageable): Page<TripContact>
    
    /**
     * 根据联系人ID查找行程关联
     */
    fun findByContactId(contactId: String, pageable: Pageable): Page<TripContact>
    
    /**
     * 根据行程ID和联系人类型查找
     */
    fun findByTripIdAndContactType(tripId: String, contactType: Int, pageable: Pageable): Page<TripContact>
    
    /**
     * 根据行程ID查找，按优先级排序
     */
    fun findByTripIdOrderByPriorityAsc(tripId: String): List<TripContact>
    
    /**
     * 检查行程和联系人是否已关联
     */
    fun existsByTripIdAndContactId(tripId: String, contactId: String): Boolean
    
    /**
     * 根据行程ID和联系人类型查找，按优先级排序
     */
    fun findByTripIdAndContactTypeOrderByPriorityAsc(tripId: String, contactType: Int): List<TripContact>
    
    /**
     * 复合查询：根据多个条件查找行程联系人关联
     */
    @Query("SELECT tc FROM TripContact tc WHERE " +
           "tc.tripId = :tripId AND " +
           "(:contactType IS NULL OR tc.contactType = :contactType) " +
           "ORDER BY tc.priority ASC")
    fun findTripContactsWithFilters(
        @Param("tripId") tripId: String,
        @Param("contactType") contactType: Int?
    ): List<TripContact>
}