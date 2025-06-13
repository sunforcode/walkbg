package org.example.meal.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.route.repository.SupplyRepository
import org.example.route.model.Supply
import java.math.BigDecimal
import java.time.Instant

@Service
@Transactional
class SupplyServiceImpl(
    private val supplyRepository: SupplyRepository
) : SupplyService {

    override fun getAllSupplies(pageable: Pageable): Page<Supply> {
        return supplyRepository.findAll(pageable)
    }

    override fun getSupplyById(id: String): Supply? {
        return supplyRepository.findById(id).orElse(null)
    }

    override fun createSupply(supply: Supply): Supply {
        return supplyRepository.save(supply)
    }

    override fun updateSupply(id: String, supply: Supply): Supply? {
        return supplyRepository.findById(id).map { existingSupply ->
            val updatedSupply = existingSupply.copy(
                name = supply.name,
                description = supply.description,
                routeId = supply.routeId,
                latitude = supply.latitude,
                longitude = supply.longitude,
                elevation = supply.elevation,
                supplyType = supply.supplyType,
                lastVerified = supply.lastVerified,
                lastVerifiedAt = supply.lastVerifiedAt,
                updatedBy = supply.updatedBy,
                updatedAt = Instant.now()
            )
            supplyRepository.save(updatedSupply)
        }.orElse(null)
    }

    override fun deleteSupply(id: String): Boolean {
        return supplyRepository.findById(id).map { supply ->
            val deletedSupply = supply.copy(
                isActive = false,
                updatedAt = Instant.now()
            )
            supplyRepository.save(deletedSupply)
            true
        }.orElse(false)
    }

    override fun getSuppliesByRoute(routeId: String, pageable: Pageable): Page<Supply> {
        return supplyRepository.findByRouteIdAndIsActiveTrue(routeId, pageable)
    }

    override fun getSuppliesByRouteSorted(routeId: String): List<Supply> {
        return supplyRepository.findByRouteIdAndIsActiveTrueOrderByElevationAsc(routeId)
    }

    override fun getSuppliesByType(supplyType: String, pageable: Pageable): Page<Supply> {
        return supplyRepository.findBySupplyTypeAndIsActiveTrue(supplyType, pageable)
    }

    override fun getSuppliesByRouteAndType(routeId: String, supplyType: String, pageable: Pageable): Page<Supply> {
        return supplyRepository.findByRouteIdAndSupplyTypeAndIsActiveTrue(routeId, supplyType, pageable)
    }

    override fun getSuppliesByElevationRange(minElevation: BigDecimal?, maxElevation: BigDecimal?, pageable: Pageable): Page<Supply> {
        return when {
            minElevation != null && maxElevation != null -> {
                supplyRepository.findByElevationBetweenAndIsActiveTrue(minElevation, maxElevation, pageable)
            }
            else -> {
                supplyRepository.findAll(pageable)
            }
        }
    }

    override fun searchSuppliesByName(name: String, pageable: Pageable): Page<Supply> {
        return supplyRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name, pageable)
    }

    override fun getSuppliesByPriceRange(priceRange: String, pageable: Pageable): Page<Supply> {
        // TODO: 价格范围功能暂时不可用，返回空页面
        return Page.empty(pageable)
    }

    override fun searchSuppliesWithFilters(
        routeId: String?,
        supplyType: String?,
        priceRange: String?,
        minElevation: BigDecimal?,
        maxElevation: BigDecimal?,
        name: String?,
        pageable: Pageable
    ): Page<Supply> {
        return supplyRepository.findSuppliesWithFilters(
            routeId, supplyType, minElevation, maxElevation, name, pageable
        )
    }

    override fun countSuppliesByRoute(routeId: String): Long {
        return supplyRepository.countByRouteIdAndIsActiveTrue(routeId)
    }

    override fun updateSupplyVerification(id: String, verifiedBy: String): Supply? {
        return supplyRepository.findById(id).map { supply ->
            val verifiedSupply = supply.copy(
                lastVerified = verifiedBy,
                lastVerifiedAt = Instant.now(),
                updatedBy = verifiedBy,
                updatedAt = Instant.now()
            )
            supplyRepository.save(verifiedSupply)
        }.orElse(null)
    }
}