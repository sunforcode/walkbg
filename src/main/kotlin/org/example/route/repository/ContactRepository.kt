package org.example.route.repository

import org.example.route.model.Contact
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface ContactRepository : JpaRepository<Contact, String> {
    
    /**
     * 根据位置查找联系人
     */
    fun findByLocationContainingIgnoreCase(location: String, pageable: Pageable): Page<Contact>
    
    /**
     * 根据验证状态查找联系人
     */
    fun findByIsVerified(isVerified: Boolean, pageable: Pageable): Page<Contact>
    
    /**
     * 根据价格范围查找联系人
     */
    fun findByPriceBetween(minPrice: BigDecimal, maxPrice: BigDecimal, pageable: Pageable): Page<Contact>
    
    /**
     * 根据姓名模糊查询联系人
     */
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Contact>
    
    /**
     * 查找已验证的联系人
     */
    fun findByIsVerifiedTrueOrderByPriceAsc(pageable: Pageable): Page<Contact>
    
    /**
     * 复合查询：根据位置和验证状态查找
     */
    @Query("SELECT c FROM Contact c WHERE " +
           "(:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:isVerified IS NULL OR c.isVerified = :isVerified) AND " +
           "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR c.price <= :maxPrice)")
    fun findContactsWithFilters(
        @Param("location") location: String?,
        @Param("isVerified") isVerified: Boolean?,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        pageable: Pageable
    ): Page<Contact>
}