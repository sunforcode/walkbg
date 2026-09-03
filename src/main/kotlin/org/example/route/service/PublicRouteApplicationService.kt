package org.example.route.service

import org.example.route.dto.PublicRouteCollectionResult
import org.example.route.dto.PublicRouteDetail
import org.example.route.dto.PublicRouteDiscoveryResult
import org.example.route.dto.PublicRouteSearchResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicRouteApplicationService(
    private val publicRouteDomainService: PublicRouteDomainService
) {
    @Transactional(readOnly = true)
    fun featured(): PublicRouteDiscoveryResult =
        PublicRouteDiscoveryResult(
            publicRouteDomainService.orderedVersions(featuredOnly = true)
                .map(publicRouteDomainService::browseSummary)
        )

    @Transactional(readOnly = true)
    fun all(): PublicRouteCollectionResult =
        PublicRouteCollectionResult(
            publicRouteDomainService.orderedVersions(featuredOnly = false)
                .map(publicRouteDomainService::browseSummary)
        )

    @Transactional(readOnly = true)
    fun search(query: String?): PublicRouteSearchResult {
        if (PublicRouteSearchRanker.normalize(query).isEmpty()) {
            return PublicRouteSearchResult(state = "initial", items = emptyList())
        }
        val candidates = publicRouteDomainService.orderedVersions(featuredOnly = false)
            .map(publicRouteDomainService::searchSummary)
        return PublicRouteSearchResult(
            state = "completed",
            items = PublicRouteSearchRanker.search(query, candidates)
        )
    }

    @Transactional(readOnly = true)
    fun detail(routeId: String): PublicRouteDetail =
        publicRouteDomainService.detail(publicRouteDomainService.findPublicVersion(routeId))
}
