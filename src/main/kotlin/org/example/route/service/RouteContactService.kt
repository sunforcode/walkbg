package org.example.route.service

import org.example.route.model.Contact
import org.example.route.model.RouteContact
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface RouteContactService {
    
    /**
     * 为路线添加联系人
     */
    fun addContactToRoute(routeId: String, contactId: String, contactType: Int, priority: Int = 0, notes: String? = null): RouteContact
    
    /**
     * 移除路线的联系人关联
     */
    fun removeContactFromRoute(routeId: String, contactId: String): Boolean
    
    /**
     * 获取路线的所有联系人
     */
    fun getContactsByRoute(routeId: String, pageable: Pageable): Page<RouteContact>
    
    /**
     * 获取路线的特定类型联系人
     */
    fun getContactsByRouteAndType(routeId: String, contactType: Int, pageable: Pageable): Page<RouteContact>
    
    /**
     * 获取路线的联系人（按优先级排序）
     */
    fun getContactsByRouteSorted(routeId: String): List<RouteContact>
    
    /**
     * 获取路线的特定类型联系人（按优先级排序）
     */
    fun getContactsByRouteAndTypeSorted(routeId: String, contactType: Int): List<RouteContact>
    
    /**
     * 获取联系人关联的路线
     */
    fun getRoutesByContact(contactId: String, pageable: Pageable): Page<RouteContact>
    
    /**
     * 更新路线联系人关联信息
     */
    fun updateRouteContact(id: String, contactType: Int?, priority: Int?, notes: String?): RouteContact?
    
    /**
     * 检查路线和联系人是否已关联
     */
    fun isContactAssociatedWithRoute(routeId: String, contactId: String): Boolean
    
    /**
     * 根据条件搜索路线联系人关联
     */
    fun searchRouteContactsWithFilters(routeId: String, contactType: Int?): List<RouteContact>
    
    /**
     * 获取路线的联系人详细信息（包含Contact实体）
     */
    fun getRouteContactsWithDetails(routeId: String): List<Contact>
    
    /**
     * 根据类型获取路线的联系人详细信息
     */
    fun getRouteContactsWithDetailsByType(routeId: String, contactType: Int): List<Contact>
}