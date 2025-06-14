package org.example.route.service

import org.example.route.model.Contact
import org.example.route.model.RouteContact
import org.example.route.repository.ContactRepository
import org.example.route.repository.RouteContactRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class RouteContactServiceImpl(
    private val routeContactRepository: RouteContactRepository,
    private val contactRepository: ContactRepository
) : RouteContactService {

    override fun addContactToRoute(routeId: String, contactId: String, contactType: Int, priority: Int, notes: String?): RouteContact {
        // 检查是否已经关联
        if (routeContactRepository.existsByRouteIdAndContactId(routeId, contactId)) {
            throw IllegalArgumentException("联系人已经与该路线关联")
        }
        
        val routeContact = RouteContact(
            id = UUID.randomUUID().toString(),
            routeId = routeId,
            contactId = contactId,
            contactType = contactType,
            priority = priority,
            notes = notes
        )
        
        return routeContactRepository.save(routeContact)
    }

    override fun removeContactFromRoute(routeId: String, contactId: String): Boolean {
        val routeContacts = routeContactRepository.findByRouteIdOrderByPriorityAsc(routeId)
        val routeContact = routeContacts.find { it.contactId == contactId }
        
        return if (routeContact != null) {
            routeContactRepository.delete(routeContact)
            true
        } else {
            false
        }
    }

    override fun getContactsByRoute(routeId: String, pageable: Pageable): Page<RouteContact> {
        return routeContactRepository.findByRouteId(routeId, pageable)
    }

    override fun getContactsByRouteAndType(routeId: String, contactType: Int, pageable: Pageable): Page<RouteContact> {
        return routeContactRepository.findByRouteIdAndContactType(routeId, contactType, pageable)
    }

    override fun getContactsByRouteSorted(routeId: String): List<RouteContact> {
        return routeContactRepository.findByRouteIdOrderByPriorityAsc(routeId)
    }

    override fun getContactsByRouteAndTypeSorted(routeId: String, contactType: Int): List<RouteContact> {
        return routeContactRepository.findByRouteIdAndContactTypeOrderByPriorityAsc(routeId, contactType)
    }

    override fun getRoutesByContact(contactId: String, pageable: Pageable): Page<RouteContact> {
        return routeContactRepository.findByContactId(contactId, pageable)
    }

    override fun updateRouteContact(id: String, contactType: Int?, priority: Int?, notes: String?): RouteContact? {
        return routeContactRepository.findById(id).map { existingRouteContact ->
            val updatedRouteContact = existingRouteContact.copy(
                contactType = contactType ?: existingRouteContact.contactType,
                priority = priority ?: existingRouteContact.priority,
                notes = notes ?: existingRouteContact.notes,
                updatedAt = Instant.now()
            )
            routeContactRepository.save(updatedRouteContact)
        }.orElse(null)
    }

    override fun isContactAssociatedWithRoute(routeId: String, contactId: String): Boolean {
        return routeContactRepository.existsByRouteIdAndContactId(routeId, contactId)
    }

    override fun searchRouteContactsWithFilters(routeId: String, contactType: Int?): List<RouteContact> {
        return routeContactRepository.findRouteContactsWithFilters(routeId, contactType)
    }

    override fun getRouteContactsWithDetails(routeId: String): List<Contact> {
        val routeContacts = routeContactRepository.findByRouteIdOrderByPriorityAsc(routeId)
        val contactIds = routeContacts.map { it.contactId }
        return contactRepository.findAllById(contactIds)
    }

    override fun getRouteContactsWithDetailsByType(routeId: String, contactType: Int): List<Contact> {
        val routeContacts = routeContactRepository.findByRouteIdAndContactTypeOrderByPriorityAsc(routeId, contactType)
        val contactIds = routeContacts.map { it.contactId }
        return contactRepository.findAllById(contactIds)
    }
}