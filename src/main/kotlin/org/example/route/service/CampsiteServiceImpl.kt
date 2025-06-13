package org.example.route.service

import org.example.route.model.Campsite
import org.example.route.repository.CampsiteRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
@Transactional
class CampsiteServiceImpl(
    private val campsiteRepository: CampsiteRepository
) : CampsiteService {

    override fun getAllCampsites(pageable: Pageable): Page<Campsite> {
        return campsiteRepository.findAll(pageable)
    }

    override fun getCampsiteById(id: String): Campsite? {
        return campsiteRepository.findById(id).orElse(null)
    }

    override fun createCampsite(campsite: Campsite): Campsite {
        return campsiteRepository.save(campsite)
    }

    override fun updateCampsite(id: String, campsite: Campsite): Campsite? {
        return campsiteRepository.findById(id).map { existingCampsite ->
            val updatedCampsite = existingCampsite.copy(
                name = campsite.name,
                description = campsite.description,
                latitude = campsite.latitude,
                longitude = campsite.longitude,
                elevation = campsite.elevation,
                campsiteType = campsite.campsiteType,
                notes = campsite.notes,
                lastVerifiedId = campsite.lastVerifiedId,
                updatedAt = Instant.now()
            )
            campsiteRepository.save(updatedCampsite)
        }.orElse(null)
    }

    override fun deleteCampsite(id: String): Boolean {
        return if (campsiteRepository.existsById(id)) {
            campsiteRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    override fun getCampsitesByRoute(routeId: String, pageable: Pageable): Page<Campsite> {
        return campsiteRepository.findByRouteIdAndIsActiveTrue(routeId, pageable)
    }

    override fun getCampsitesByRouteSorted(routeId: String): List<Campsite> {
        return campsiteRepository.findByRouteIdAndIsActiveTrueOrderByElevationAsc(routeId)
    }

    override fun getCampsitesByType(campsiteType: Int, pageable: Pageable): Page<Campsite> {
        return campsiteRepository.findByCampsiteTypeAndIsActiveTrue(campsiteType, pageable)
    }

    override fun getCampsitesByRouteAndType(routeId: String, campsiteType: Int, pageable: Pageable): Page<Campsite> {
        return campsiteRepository.findByRouteIdAndCampsiteTypeAndIsActiveTrue(routeId, campsiteType, pageable)
    }

    override fun getCampsitesByElevationRange(minElevation: BigDecimal?, maxElevation: BigDecimal?, pageable: Pageable): Page<Campsite> {
        return when {
            minElevation != null && maxElevation != null -> {
                campsiteRepository.findByElevationBetweenAndIsActiveTrue(minElevation, maxElevation, pageable)
            }
            else -> {
                campsiteRepository.findAll(pageable)
            }
        }
    }

    override fun searchCampsitesByName(name: String, pageable: Pageable): Page<Campsite> {
        return campsiteRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name, pageable)
    }

    override fun searchCampsitesWithFilters(
        routeId: String?,
        campsiteType: Int?,
        minElevation: BigDecimal?,
        maxElevation: BigDecimal?,
        name: String?,
        pageable: Pageable
    ): Page<Campsite> {
        return campsiteRepository.findCampsitesWithFilters(routeId, campsiteType, minElevation, maxElevation, name, pageable)
    }

    override fun countCampsitesByRoute(routeId: String): Long {
        return campsiteRepository.countByRouteIdAndIsActiveTrue(routeId)
    }

    override fun verifyCampsite(id: String, verifiedBy: String): Campsite? {
        return campsiteRepository.findById(id).map { campsite ->
            val verifiedCampsite = campsite.copy(
                lastVerifiedId = verifiedBy,
                updatedAt = Instant.now()
            )
            campsiteRepository.save(verifiedCampsite)
        }.orElse(null)
    }
}