package org.example.route.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.route.dto.PublicRouteCollectionResult
import org.example.route.dto.PublicRouteDetail
import org.example.route.dto.PublicRouteDiscoveryResult
import org.example.route.dto.PublicRouteSearchResult
import org.example.route.service.PublicRouteApplicationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/public-routes")
class PublicRouteController(
    private val publicRouteApplicationService: PublicRouteApplicationService
) {
    @GetMapping("/featured")
    fun featured(request: HttpServletRequest): DataResponse<PublicRouteDiscoveryResult> {
        rejectUnknownQueryParameters(request, emptySet())
        return DataResponse(publicRouteApplicationService.featured())
    }

    @GetMapping
    fun all(request: HttpServletRequest): DataResponse<PublicRouteCollectionResult> {
        rejectUnknownQueryParameters(request, emptySet())
        return DataResponse(publicRouteApplicationService.all())
    }

    @GetMapping("/search")
    fun search(
        request: HttpServletRequest,
        @RequestParam(required = false) query: String?
    ): DataResponse<PublicRouteSearchResult> {
        rejectUnknownQueryParameters(request, setOf("query"))
        return DataResponse(publicRouteApplicationService.search(query))
    }

    @GetMapping("/{routeId}")
    fun detail(
        request: HttpServletRequest,
        @PathVariable routeId: String
    ): DataResponse<PublicRouteDetail> {
        rejectUnknownQueryParameters(request, emptySet())
        return DataResponse(publicRouteApplicationService.detail(routeId))
    }

    private fun rejectUnknownQueryParameters(request: HttpServletRequest, allowed: Set<String>) {
        if (request.parameterMap.keys.any { it !in allowed }) {
            throw ApiContractException.invalidRequest("请求包含未定义参数")
        }
    }
}
