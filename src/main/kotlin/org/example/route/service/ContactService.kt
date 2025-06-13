package org.example.route.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.example.route.model.Contact
import java.math.BigDecimal

interface ContactService {
    
    /**
     * 获取所有联系人（分页）
     */
    fun getAllContacts(pageable: Pageable): Page<Contact>
    
    /**
     * 根据ID获取联系人
     */
    fun getContactById(id: String): Contact?
    
    /**
     * 创建新联系人
     */
    fun createContact(contact: Contact): Contact
    
    /**
     * 更新联系人信息
     */
    fun updateContact(id: String, contact: Contact): Contact?
    
    /**
     * 删除联系人
     */
    fun deleteContact(id: String): Boolean
    
    /**
     * 根据位置搜索联系人
     */
    fun searchContactsByLocation(location: String, pageable: Pageable): Page<Contact>
    
    /**
     * 根据验证状态获取联系人
     */
    fun getContactsByVerificationStatus(isVerified: Boolean, pageable: Pageable): Page<Contact>
    
    /**
     * 根据价格范围获取联系人
     */
    fun getContactsByPriceRange(minPrice: BigDecimal?, maxPrice: BigDecimal?, pageable: Pageable): Page<Contact>
    
    /**
     * 根据姓名搜索联系人
     */
    fun searchContactsByName(name: String, pageable: Pageable): Page<Contact>
    
    /**
     * 获取已验证的联系人（按价格排序）
     */
    fun getVerifiedContacts(pageable: Pageable): Page<Contact>
    
    /**
     * 复合条件搜索联系人
     */
    fun searchContactsWithFilters(
        location: String?,
        isVerified: Boolean?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        pageable: Pageable
    ): Page<Contact>
    
    /**
     * 验证联系人
     */
    fun verifyContact(id: String): Contact?
    
    /**
     * 取消验证联系人
     */
    fun unverifyContact(id: String): Contact?
}