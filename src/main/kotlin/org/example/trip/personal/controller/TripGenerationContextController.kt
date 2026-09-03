package org.example.trip.personal.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.DataResponse
import org.example.trip.personal.dto.TripGenerationContextProjection
import org.example.trip.personal.service.PersonalTripApplicationService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/public-routes")
class TripGenerationContextController(
    private val service: PersonalTripApplicationService
) {
    @GetMapping("/{routeId}/trip-generation-context")
    fun context(
        authentication: Authentication?,
        @PathVariable routeId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<TripGenerationContextProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.generationContext(authentication.accountPrincipal().userId, routeId))
    }
}
