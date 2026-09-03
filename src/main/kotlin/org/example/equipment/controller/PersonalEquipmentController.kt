package org.example.equipment.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.equipment.dto.CreatePersonalEquipmentRequest
import org.example.equipment.dto.PersonalEquipmentCollectionResponse
import org.example.equipment.dto.PersonalEquipmentDeletionImpact
import org.example.equipment.dto.PersonalEquipmentDeletionResponse
import org.example.equipment.dto.PersonalEquipmentMutationResponse
import org.example.equipment.dto.PersonalEquipmentProjection
import org.example.equipment.dto.UpdatePersonalEquipmentRequest
import org.example.equipment.service.CreatePersonalEquipmentCommand
import org.example.equipment.service.PersonalEquipmentApplicationService
import org.example.equipment.service.UpdatePersonalEquipmentCommand
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
@RequestMapping("/api/v1/personal-equipment")
class PersonalEquipmentController(
    private val service: PersonalEquipmentApplicationService
) {
    @GetMapping
    fun collection(
        authentication: Authentication?,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentCollectionResponse> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getEquipmentCollection(authentication.accountPrincipal().userId))
    }

    @PostMapping
    fun create(
        authentication: Authentication?,
        @RequestBody request: CreatePersonalEquipmentRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<DataResponse<PersonalEquipmentMutationResponse>> {
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        if (!request.hasName || !request.hasOwnedQuantity || request.name == null || request.ownedQuantity == null) {
            throw ApiContractException.invalidRequest("新增装备必须包含 name 和 ownedQuantity")
        }
        if (request.hasUnitWeight && request.unitWeight == null) {
            throw ApiContractException.invalidRequest("新增装备的 unitWeight 不能为 null")
        }
        request.unitWeight?.let {
            rejectUnknownFields(it)
            requireWeightGrams(it)
        }
        val response = service.createEquipment(
            authentication.accountPrincipal().userId,
            CreatePersonalEquipmentCommand(
                request.name,
                request.ownedQuantity,
                request.unitWeight?.grams
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse(response))
    }

    @GetMapping("/{personalEquipmentId}")
    fun detail(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getEquipment(authentication.accountPrincipal().userId, personalEquipmentId))
    }

    @PatchMapping("/{personalEquipmentId}")
    fun update(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        @RequestBody request: UpdatePersonalEquipmentRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentMutationResponse> {
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        if (!request.hasName && !request.hasOwnedQuantity && !request.hasUnitWeight) {
            throw ApiContractException.unprocessable("validation_failed", "至少提供一个可修改字段")
        }
        if (request.hasName && request.name == null || request.hasOwnedQuantity && request.ownedQuantity == null) {
            throw ApiContractException.invalidRequest("name 和 ownedQuantity 不能为 null")
        }
        request.unitWeight?.let {
            rejectUnknownFields(it)
            requireWeightGrams(it)
        }
        return DataResponse(
            service.updateEquipment(
                authentication.accountPrincipal().userId,
                personalEquipmentId,
                UpdatePersonalEquipmentCommand(
                    name = request.name,
                    ownedQuantity = request.ownedQuantity,
                    unitWeightGrams = request.unitWeight?.grams,
                    hasName = request.hasName,
                    hasOwnedQuantity = request.hasOwnedQuantity,
                    hasUnitWeight = request.hasUnitWeight,
                    clearUnitWeight = request.hasUnitWeight && request.unitWeight == null
                )
            )
        )
    }

    @GetMapping("/{personalEquipmentId}/deletion-impact")
    fun deletionImpact(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentDeletionImpact> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getDeletionImpact(authentication.accountPrincipal().userId, personalEquipmentId))
    }

    @DeleteMapping("/{personalEquipmentId}")
    fun delete(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentDeletionResponse> {
        rejectUnknownQuery(servletRequest, setOf("confirmListRemoval"))
        val values = servletRequest.parameterMap["confirmListRemoval"]
        if (values != null && values.size != 1) throw ApiContractException.invalidRequest("确认参数只能出现一次")
        if (values?.singleOrNull() != "true") {
            throw ApiContractException.conflict(
                "deletion_confirmation_required",
                "删除装备前必须确认从所有清单移除"
            )
        }
        return DataResponse(
            service.deleteEquipment(
                authentication.accountPrincipal().userId,
                personalEquipmentId,
                confirmListRemoval = true
            )
        )
    }
}

internal fun rejectUnknownQuery(request: HttpServletRequest, allowed: Set<String> = emptySet()) {
    if (request.parameterMap.keys.any { it !in allowed }) {
        throw ApiContractException.invalidRequest("请求包含未定义查询参数")
    }
}

internal fun rejectUnknownFields(request: org.example.equipment.dto.EquipmentStrictRequest) {
    if (request.hasUnknownFields()) {
        throw ApiContractException.invalidRequest("请求包含未定义字段")
    }
}

private fun requireWeightGrams(request: org.example.equipment.dto.UnitWeightRequest) {
    if (request.grams == null) {
        throw ApiContractException.invalidRequest("unitWeight 必须包含 grams")
    }
}
