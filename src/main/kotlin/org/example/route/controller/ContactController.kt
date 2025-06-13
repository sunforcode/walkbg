package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.example.route.service.ContactService
import org.example.route.service.RouteContactService
import org.example.route.model.Contact
import org.example.route.model.RouteContact
import java.math.BigDecimal

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "联系人管理", description = "联系人相关的API接口")
class ContactController(
    private val contactService: ContactService,
    private val routeContactService: RouteContactService
) {

    @GetMapping
    @Operation(summary = "获取联系人列表", description = "分页获取所有联系人")
    fun getAllContacts(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<Page<Contact>> {
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        val pageable: Pageable = PageRequest.of(page, size, sort)
        val contacts = contactService.getAllContacts(pageable)
        return ResponseEntity.ok(contacts)
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取联系人", description = "根据联系人ID获取详细信息")
    fun getContactById(
        @Parameter(description = "联系人ID") @PathVariable id: String
    ): ResponseEntity<Contact> {
        val contact = contactService.getContactById(id)
        return if (contact != null) {
            ResponseEntity.ok(contact)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @Operation(summary = "创建新联系人", description = "创建一个新的联系人记录")
    fun createContact(
        @RequestBody contact: Contact
    ): ResponseEntity<Contact> {
        val createdContact = contactService.createContact(contact)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdContact)
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新联系人信息", description = "根据ID更新联系人信息")
    fun updateContact(
        @Parameter(description = "联系人ID") @PathVariable id: String,
        @RequestBody contact: Contact
    ): ResponseEntity<Contact> {
        val updatedContact = contactService.updateContact(id, contact)
        return if (updatedContact != null) {
            ResponseEntity.ok(updatedContact)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除联系人", description = "根据ID删除联系人")
    fun deleteContact(
        @Parameter(description = "联系人ID") @PathVariable id: String
    ): ResponseEntity<Void> {
        val deleted = contactService.deleteContact(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search/location")
    @Operation(summary = "根据位置搜索联系人", description = "根据位置关键词搜索联系人")
    fun searchContactsByLocation(
        @Parameter(description = "位置关键词") @RequestParam location: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<Contact>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val contacts = contactService.searchContactsByLocation(location, pageable)
        return ResponseEntity.ok(contacts)
    }

    @GetMapping("/search/name")
    @Operation(summary = "根据姓名搜索联系人", description = "根据姓名关键词搜索联系人")
    fun searchContactsByName(
        @Parameter(description = "姓名关键词") @RequestParam name: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<Contact>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val contacts = contactService.searchContactsByName(name, pageable)
        return ResponseEntity.ok(contacts)
    }

    @GetMapping("/verified")
    @Operation(summary = "获取已验证的联系人", description = "获取所有已验证的联系人，按价格升序排列")
    fun getVerifiedContacts(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<Contact>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val contacts = contactService.getVerifiedContacts(pageable)
        return ResponseEntity.ok(contacts)
    }

    @GetMapping("/search")
    @Operation(summary = "复合条件搜索联系人", description = "根据多个条件搜索联系人")
    fun searchContactsWithFilters(
        @Parameter(description = "位置关键词") @RequestParam(required = false) location: String?,
        @Parameter(description = "是否已验证") @RequestParam(required = false) isVerified: Boolean?,
        @Parameter(description = "最低价格") @RequestParam(required = false) minPrice: BigDecimal?,
        @Parameter(description = "最高价格") @RequestParam(required = false) maxPrice: BigDecimal?,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<Contact>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val contacts = contactService.searchContactsWithFilters(location, isVerified, minPrice, maxPrice, pageable)
        return ResponseEntity.ok(contacts)
    }

    @PatchMapping("/{id}/verify")
    @Operation(summary = "验证联系人", description = "将联系人标记为已验证")
    fun verifyContact(
        @Parameter(description = "联系人ID") @PathVariable id: String
    ): ResponseEntity<Contact> {
        val verifiedContact = contactService.verifyContact(id)
        return if (verifiedContact != null) {
            ResponseEntity.ok(verifiedContact)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/unverify")
    @Operation(summary = "取消验证联系人", description = "将联系人标记为未验证")
    fun unverifyContact(
        @Parameter(description = "联系人ID") @PathVariable id: String
    ): ResponseEntity<Contact> {
        val unverifiedContact = contactService.unverifyContact(id)
        return if (unverifiedContact != null) {
            ResponseEntity.ok(unverifiedContact)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ========== 路线联系人关联相关API ==========

    @PostMapping("/routes/{routeId}/contacts/{contactId}")
    @Operation(summary = "为路线添加联系人", description = "将联系人关联到指定路线")
    fun addContactToRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Parameter(description = "联系人ID") @PathVariable contactId: String,
        @Parameter(description = "联系人类型：0-向导，1-接送服务，2-住宿联系人，3-紧急联系人，4-其他") @RequestParam(defaultValue = "0") contactType: Int,
        @Parameter(description = "优先级，数字越小优先级越高") @RequestParam(defaultValue = "0") priority: Int,
        @Parameter(description = "备注信息") @RequestParam(required = false) notes: String?
    ): ResponseEntity<RouteContact> {
        return try {
            val routeContact = routeContactService.addContactToRoute(routeId, contactId, contactType, priority, notes)
            ResponseEntity.status(HttpStatus.CREATED).body(routeContact)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/routes/{routeId}/contacts/{contactId}")
    @Operation(summary = "移除路线的联系人", description = "移除路线与联系人的关联")
    fun removeContactFromRoute(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Parameter(description = "联系人ID") @PathVariable contactId: String
    ): ResponseEntity<Void> {
        val removed = routeContactService.removeContactFromRoute(routeId, contactId)
        return if (removed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/routes/{routeId}/contacts")
    @Operation(summary = "获取路线的联系人", description = "获取指定路线的所有联系人详细信息")
    fun getRouteContacts(
        @Parameter(description = "路线ID") @PathVariable routeId: String
    ): ResponseEntity<List<Contact>> {
        val contacts = routeContactService.getRouteContactsWithDetails(routeId)
        return ResponseEntity.ok(contacts)
    }

    @GetMapping("/routes/{routeId}/contacts/type/{contactType}")
    @Operation(summary = "获取路线的特定类型联系人", description = "获取指定路线的特定类型联系人详细信息")
    fun getRouteContactsByType(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Parameter(description = "联系人类型") @PathVariable contactType: Int
    ): ResponseEntity<List<Contact>> {
        val contacts = routeContactService.getRouteContactsWithDetailsByType(routeId, contactType)
        return ResponseEntity.ok(contacts)
    }

    @GetMapping("/routes/{routeId}/contacts/associations")
    @Operation(summary = "获取路线联系人关联信息", description = "获取路线与联系人的关联详细信息")
    fun getRouteContactAssociations(
        @Parameter(description = "路线ID") @PathVariable routeId: String,
        @Parameter(description = "联系人类型") @RequestParam(required = false) contactType: Int?
    ): ResponseEntity<List<RouteContact>> {
        val associations = routeContactService.searchRouteContactsWithFilters(routeId, contactType)
        return ResponseEntity.ok(associations)
    }
}