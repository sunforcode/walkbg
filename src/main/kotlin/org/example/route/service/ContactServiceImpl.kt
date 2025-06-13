package org.example.route.service

import org.example.route.model.Contact
import org.example.route.repository.ContactRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
@Transactional
class ContactServiceImpl(
    private val contactRepository: ContactRepository
) : ContactService {

    override fun getAllContacts(pageable: Pageable): Page<Contact> {
        return contactRepository.findAll(pageable)
    }

    override fun getContactById(id: String): Contact? {
        return contactRepository.findById(id).orElse(null)
    }

    override fun createContact(contact: Contact): Contact {
        return contactRepository.save(contact)
    }

    override fun updateContact(id: String, contact: Contact): Contact? {
        return contactRepository.findById(id).map { existingContact ->
            val updatedContact = existingContact.copy(
                name = contact.name,
                phone = contact.phone,
                description = contact.description,
                location = contact.location,
                price = contact.price,
                isVerified = contact.isVerified,
                updatedAt = Instant.now()
            )
            contactRepository.save(updatedContact)
        }.orElse(null)
    }

    override fun deleteContact(id: String): Boolean {
        return if (contactRepository.existsById(id)) {
            contactRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    override fun searchContactsByLocation(location: String, pageable: Pageable): Page<Contact> {
        return contactRepository.findByLocationContainingIgnoreCase(location, pageable)
    }

    override fun getContactsByVerificationStatus(isVerified: Boolean, pageable: Pageable): Page<Contact> {
        return contactRepository.findByIsVerified(isVerified, pageable)
    }

    override fun getContactsByPriceRange(minPrice: BigDecimal?, maxPrice: BigDecimal?, pageable: Pageable): Page<Contact> {
        return when {
            minPrice != null && maxPrice != null -> {
                contactRepository.findByPriceBetween(minPrice, maxPrice, pageable)
            }
            else -> {
                contactRepository.findAll(pageable)
            }
        }
    }

    override fun searchContactsByName(name: String, pageable: Pageable): Page<Contact> {
        return contactRepository.findByNameContainingIgnoreCase(name, pageable)
    }

    override fun getVerifiedContacts(pageable: Pageable): Page<Contact> {
        return contactRepository.findByIsVerifiedTrueOrderByPriceAsc(pageable)
    }

    override fun searchContactsWithFilters(
        location: String?,
        isVerified: Boolean?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        pageable: Pageable
    ): Page<Contact> {
        return contactRepository.findContactsWithFilters(location, isVerified, minPrice, maxPrice, pageable)
    }

    override fun verifyContact(id: String): Contact? {
        return contactRepository.findById(id).map { contact ->
            val verifiedContact = contact.copy(
                isVerified = true,
                updatedAt = Instant.now()
            )
            contactRepository.save(verifiedContact)
        }.orElse(null)
    }

    override fun unverifyContact(id: String): Contact? {
        return contactRepository.findById(id).map { contact ->
            val unverifiedContact = contact.copy(
                isVerified = false,
                updatedAt = Instant.now()
            )
            contactRepository.save(unverifiedContact)
        }.orElse(null)
    }
}