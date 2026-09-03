package org.example.trip.personal.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.trip.personal.dto.CreateTripEquipmentItemCommand
import org.example.trip.personal.dto.CreateTripEquipmentItemRequest
import org.example.trip.personal.dto.TripEquipmentProjection
import org.example.trip.personal.dto.TripEquipmentStrictRequest
import org.example.trip.personal.dto.UpdateTripEquipmentItemCommand
import org.example.trip.personal.dto.UpdateTripEquipmentItemRequest
import org.example.trip.personal.service.TripEquipmentApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
class TripEquipmentController(
    private val service: TripEquipmentApplicationService
) {
    @GetMapping("/{tripId}/equipment")
    fun get(
        authentication: Authentication?,
        @PathVariable tripId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<TripEquipmentProjection> {
        rejectTripEquipmentQuery(servletRequest)
        return DataResponse(service.getEquipment(authentication.accountPrincipal().userId, tripId))
    }

    @PostMapping("/{tripId}/equipment/items")
    fun create(
        authentication: Authentication?,
        @PathVariable tripId: String,
        @RequestBody request: CreateTripEquipmentItemRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectTripEquipmentQuery(servletRequest)
        rejectTripEquipmentUnknownFields(request)
        if (!request.hasName || !request.hasQuantity || request.name == null || request.quantity == null) {
            throw ApiContractException.invalidRequest("新增本次装备必须包含 name 和 quantity")
        }
        if (request.hasUnitWeight && request.unitWeight == null) {
            throw ApiContractException.invalidRequest("新增本次装备的 unitWeight 不能为 null")
        }
        if (request.hasNote && request.note == null) {
            throw ApiContractException.invalidRequest("新增本次装备的 note 不能为 null")
        }
        request.unitWeight?.let(::requireTripEquipmentWeight)
        val result = service.createItem(
            authentication.accountPrincipal().userId,
            tripId,
            CreateTripEquipmentItemCommand(
                name = request.name,
                quantity = request.quantity,
                unitWeightGrams = request.unitWeight?.grams,
                note = request.note
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse(result))
    }

    @PatchMapping("/{tripId}/equipment/items/{itemId}")
    fun update(
        authentication: Authentication?,
        @PathVariable tripId: String,
        @PathVariable itemId: String,
        @RequestBody request: UpdateTripEquipmentItemRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<TripEquipmentProjection> {
        rejectTripEquipmentQuery(servletRequest)
        rejectTripEquipmentUnknownFields(request)
        if (!request.hasName && !request.hasQuantity && !request.hasUnitWeight && !request.hasNote) {
            throw ApiContractException.unprocessable("validation_failed", "至少提供一个可修改字段")
        }
        if (request.hasName && request.name == null || request.hasQuantity && request.quantity == null) {
            throw ApiContractException.invalidRequest("name 和 quantity 不能为 null")
        }
        request.unitWeight?.let(::requireTripEquipmentWeight)
        return DataResponse(
            service.updateItem(
                authentication.accountPrincipal().userId,
                tripId,
                itemId,
                UpdateTripEquipmentItemCommand(
                    name = request.name,
                    quantity = request.quantity,
                    unitWeightGrams = request.unitWeight?.grams,
                    note = request.note,
                    hasName = request.hasName,
                    hasQuantity = request.hasQuantity,
                    hasUnitWeight = request.hasUnitWeight,
                    hasNote = request.hasNote
                )
            )
        )
    }

    @DeleteMapping("/{tripId}/equipment/items/{itemId}")
    fun delete(
        authentication: Authentication?,
        @PathVariable tripId: String,
        @PathVariable itemId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<TripEquipmentProjection> {
        rejectTripEquipmentQuery(servletRequest)
        rejectTripEquipmentBody(servletRequest, "删除本次装备不接受请求体")
        return DataResponse(service.deleteItem(authentication.accountPrincipal().userId, tripId, itemId))
    }

    @PostMapping("/{tripId}/equipment:recheck-ownership")
    fun recheckOwnership(
        authentication: Authentication?,
        @PathVariable tripId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<TripEquipmentProjection> {
        rejectTripEquipmentQuery(servletRequest)
        rejectTripEquipmentBody(servletRequest, "重新核对拥有状态只接受空请求体")
        return DataResponse(service.recheckOwnership(authentication.accountPrincipal().userId, tripId))
    }
}

private fun rejectTripEquipmentBody(request: HttpServletRequest, message: String) {
    if (request.inputStream.read() != -1) {
        throw ApiContractException.invalidRequest(message)
    }
}

private fun rejectTripEquipmentQuery(request: HttpServletRequest) {
    if (request.parameterMap.isNotEmpty()) {
        throw ApiContractException.invalidRequest("请求包含未定义查询参数")
    }
}

private fun rejectTripEquipmentUnknownFields(request: TripEquipmentStrictRequest) {
    if (request.hasUnknownFields()) {
        throw ApiContractException.invalidRequest("请求包含未定义字段")
    }
}

private fun requireTripEquipmentWeight(request: org.example.trip.personal.dto.TripEquipmentWeightRequest) {
    rejectTripEquipmentUnknownFields(request)
    if (request.grams == null) {
        throw ApiContractException.invalidRequest("unitWeight 必须包含 grams")
    }
}
