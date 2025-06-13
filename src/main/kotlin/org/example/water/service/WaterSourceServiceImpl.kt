package org.example.water.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.water.repository.WaterSourceRepository
import org.example.water.model.WaterSource
import java.math.BigDecimal
import java.time.Instant

@Service
@Transactional
class WaterSourceServiceImpl(
    private val waterSourceRepository: WaterSourceRepository
) : WaterSourceService {

    override fun getAllWaterSources(pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findAll(pageable)
    }

    override fun getWaterSourceById(id: String): WaterSource? {
        return waterSourceRepository.findById(id).orElse(null)
    }

    override fun createWaterSource(waterSource: WaterSource): WaterSource {
        return waterSourceRepository.save(waterSource)
    }

    override fun updateWaterSource(id: String, waterSource: WaterSource): WaterSource? {
        return waterSourceRepository.findById(id).map { existingWaterSource ->
            val updatedWaterSource = existingWaterSource.copy(
                name = waterSource.name,
                description = waterSource.description,
                latitude = waterSource.latitude,
                longitude = waterSource.longitude,
                elevation = waterSource.elevation,
                waterType = waterSource.waterType,
                waterQuality = waterSource.waterQuality,
                reliability = waterSource.reliability,
                requiresTreatment = waterSource.requiresTreatment,
                notes = waterSource.notes,
                lastVerified = waterSource.lastVerified,
                updatedAt = Instant.now()
            )
            waterSourceRepository.save(updatedWaterSource)
        }.orElse(null)
    }

    override fun deleteWaterSource(id: String): Boolean {
        return waterSourceRepository.findById(id).map { waterSource ->
            val deletedWaterSource = waterSource.copy(
                isActive = false,
                updatedAt = Instant.now()
            )
            waterSourceRepository.save(deletedWaterSource)
            true
        }.orElse(false)
    }

    override fun getWaterSourcesByRoute(routeId: String, pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findByRouteId(routeId, pageable)
    }

    override fun getWaterSourcesByRouteSorted(routeId: String): List<WaterSource> {
        return waterSourceRepository.findByRouteIdAndIsActiveTrueOrderByElevationAsc(routeId)
    }

    override fun getWaterSourcesByType(waterType: Int, pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findByWaterTypeAndIsActiveTrue(waterType, pageable)
    }

    override fun getWaterSourcesByQuality(waterQuality: Int, pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findByWaterQualityAndIsActiveTrue(waterQuality, pageable)
    }

    override fun getWaterSourcesByRouteAndType(routeId: String, waterType: Int, pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findByRouteIdAndWaterTypeAndIsActiveTrue(routeId, waterType, pageable)
    }

    override fun getWaterSourcesByElevationRange(minElevation: BigDecimal?, maxElevation: BigDecimal?, pageable: Pageable): Page<WaterSource> {
        return when {
            minElevation != null && maxElevation != null -> {
                waterSourceRepository.findByElevationBetweenAndIsActiveTrue(minElevation, maxElevation, pageable)
            }
            else -> {
                waterSourceRepository.findAll(pageable)
            }
        }
    }

    override fun searchWaterSourcesByName(name: String, pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name, pageable)
    }

    override fun getSafeWaterSources(pageable: Pageable): Page<WaterSource> {
        return waterSourceRepository.findByRequiresTreatmentFalseAndIsActiveTrue(pageable)
    }

    override fun searchWaterSourcesWithFilters(
        routeId: String?,
        waterType: Int?,
        waterQuality: Int?,
        minElevation: BigDecimal?,
        maxElevation: BigDecimal?,
        requiresTreatment: Boolean?,
        name: String?,
        pageable: Pageable
    ): Page<WaterSource> {
        return waterSourceRepository.findWaterSourcesWithFilters(
            routeId, waterType, waterQuality, minElevation, maxElevation, requiresTreatment, name, pageable
        )
    }

    override fun countWaterSourcesByRoute(routeId: String): Long {
        return waterSourceRepository.countByRouteIdAndIsActiveTrue(routeId)
    }

    override fun verifyWaterSource(id: String, verifiedBy: String): WaterSource? {
        return waterSourceRepository.findById(id).map { waterSource ->
            val verifiedWaterSource = waterSource.copy(
                lastVerified = verifiedBy, // lastVerified字段是String类型，存储验证者ID
                updatedAt = Instant.now()
            )
            waterSourceRepository.save(verifiedWaterSource)
        }.orElse(null)
    }
}