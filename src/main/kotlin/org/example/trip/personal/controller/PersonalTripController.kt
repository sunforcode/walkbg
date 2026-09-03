package org.example.trip.personal.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.trip.personal.dto.CancelTripCommand
import org.example.trip.personal.dto.CancelTripRequest
import org.example.trip.personal.dto.GenerateTripCommand
import org.example.trip.personal.dto.GenerateTripRequest
import org.example.trip.personal.dto.GenerateTripResult
import org.example.trip.personal.dto.MigrateTripCommand
import org.example.trip.personal.dto.MigrateTripRequest
import org.example.trip.personal.dto.PersonalTripCalendarProjection
import org.example.trip.personal.dto.PersonalTripCollectionProjection
import org.example.trip.personal.dto.PersonalTripDaysProjection
import org.example.trip.personal.dto.PersonalTripDetailProjection
import org.example.trip.personal.dto.PersonalTripFocusProjection
import org.example.trip.personal.dto.PersonalTripStrictRequest
import org.example.trip.personal.dto.PersonalTripWeatherProjection
import org.example.trip.personal.dto.TransportSelectionCommand
import org.example.trip.personal.dto.TripRouteVersionStatusProjection
import org.example.trip.personal.service.PersonalTripApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
class PersonalTripController(
    private val service: PersonalTripApplicationService
) {
    @GetMapping("/focus")
    fun focus(
        authentication: Authentication?,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripFocusProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.focus(authentication.accountPrincipal().userId))
    }

    @GetMapping
    fun collection(
        authentication: Authentication?,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripCollectionProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.collection(authentication.accountPrincipal().userId))
    }

    @GetMapping("/calendar")
    fun calendar(
        authentication: Authentication?,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripCalendarProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.calendar(authentication.accountPrincipal().userId))
    }

    @PostMapping
    fun generate(
        authentication: Authentication?,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestBody request: GenerateTripRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<DataResponse<GenerateTripResult>> {
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        val key = requireIdempotencyKey(idempotencyKey)
        val routeId = requireField(request.routeId, "routeId")
        val routeVersionId = requireField(request.routeVersionId, "routeVersionId")
        val departureCity = requireField(request.departureCity, "departureCity")
        val startDate = request.startDate ?: throw ApiContractException.invalidRequest("startDate 为必填字段")
        val selection = request.transportSelection?.let {
            rejectUnknownFields(it)
            TransportSelectionCommand(
                selectionId = requireField(it.selectionId, "selectionId"),
                transportOptionId = requireField(it.transportOptionId, "transportOptionId")
            )
        }
        val result = service.generate(
            authentication.accountPrincipal().userId,
            key,
            GenerateTripCommand(
                routeId = routeId,
                routeVersionId = routeVersionId,
                departureCity = departureCity,
                startDate = startDate,
                equipmentListId = request.equipmentListId,
                transportSelection = selection
            )
        )
        val status = if (result is GenerateTripResult.TripCreated) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(DataResponse(result))
    }

    @GetMapping("/{tripId}")
    fun detail(
        authentication: Authentication?,
        @PathVariable tripId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripDetailProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.detail(authentication.accountPrincipal().userId, tripId))
    }

    @GetMapping("/{tripId}/days")
    fun days(
        authentication: Authentication?,
        @PathVariable tripId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripDaysProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.days(authentication.accountPrincipal().userId, tripId))
    }

    @GetMapping("/{tripId}/weather")
    fun weather(
        authentication: Authentication?,
        @PathVariable tripId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripWeatherProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.weather(authentication.accountPrincipal().userId, tripId))
    }

    @PostMapping("/{tripId}/cancellations")
    fun cancel(
        authentication: Authentication?,
        @PathVariable tripId: String,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestBody request: CancelTripRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripDetailProjection> {
        val key = requireIdempotencyKey(idempotencyKey)
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        val expectedRevision = requireField(request.expectedRevision, "expectedRevision")
        return DataResponse(
            service.cancel(
                authentication.accountPrincipal().userId,
                tripId,
                key,
                CancelTripCommand(expectedRevision, request.confirmed == true)
            )
        )
    }

    @GetMapping("/{tripId}/route-version-status")
    fun routeVersionStatus(
        authentication: Authentication?,
        @PathVariable tripId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<TripRouteVersionStatusProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.routeVersionStatus(authentication.accountPrincipal().userId, tripId))
    }

    @PostMapping("/{tripId}/route-migrations")
    fun migrate(
        authentication: Authentication?,
        @PathVariable tripId: String,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestBody request: MigrateTripRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalTripDetailProjection> {
        val key = requireIdempotencyKey(idempotencyKey)
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        return DataResponse(
            service.migrate(
                authentication.accountPrincipal().userId,
                tripId,
                key,
                MigrateTripCommand(
                    expectedRevision = requireField(request.expectedRevision, "expectedRevision"),
                    targetRouteVersionId = requireField(request.targetRouteVersionId, "targetRouteVersionId")
                )
            )
        )
    }
}

internal fun rejectUnknownQuery(request: HttpServletRequest) {
    if (request.parameterMap.isNotEmpty()) {
        throw ApiContractException.invalidRequest("请求包含未定义查询参数")
    }
}

internal fun rejectUnknownFields(request: PersonalTripStrictRequest) {
    if (request.hasUnknownFields()) {
        throw ApiContractException.invalidRequest("请求包含未定义字段")
    }
}

private fun requireIdempotencyKey(value: String?): String =
    value?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw ApiContractException.invalidRequest("缺少 Idempotency-Key")

private fun requireField(value: String?, name: String): String =
    value?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw ApiContractException.invalidRequest("$name 为必填字段")
