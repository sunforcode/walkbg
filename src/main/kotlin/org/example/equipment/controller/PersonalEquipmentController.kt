package org.example.equipment.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.equipment.dto.*
import org.example.equipment.service.*
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
@RequestMapping("/api/v1")
class PersonalEquipmentController(
    private val service: PersonalEquipmentApplicationService
) {
    @GetMapping("/personal-equipment")
    fun collection(
        authentication: Authentication?,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentCollectionResponse> {
        rejectUnknownQuery(servletRequest, setOf("keyword", "category", "equipmentType", "missingWeight"))
        if (servletRequest.parameterMap.any { it.value.size != 1 || it.value[0].isBlank() }) {
            throw ApiContractException.invalidRequest("筛选参数不得重复或为空")
        }
        val missingWeight = servletRequest.getParameter("missingWeight")?.let {
            when (it) { "true" -> true; "false" -> false; else -> throw ApiContractException.invalidRequest("missingWeight 必须为 true 或 false") }
        }
        return DataResponse(service.getEquipmentCollection(authentication.accountPrincipal().userId, PersonalEquipmentFilter(
            keyword = servletRequest.getParameter("keyword"), category = servletRequest.getParameter("category"),
            equipmentType = servletRequest.getParameter("equipmentType"), missingWeight = missingWeight
        )))
    }

    @PostMapping("/personal-equipment")
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
        if (request.hasUnitWeight && request.unitWeight == null || request.hasCategory && request.category == null ||
            request.hasEquipmentType && request.equipmentType == null || request.hasVariant && request.variant == null ||
            request.hasUnitLabel && request.unitLabel == null || request.hasWeightScope && request.weightScope == null) {
            throw ApiContractException.invalidRequest("新增装备的可选字段不能为 null")
        }
        val response = service.createEquipment(
            authentication.accountPrincipal().userId,
            CreatePersonalEquipmentCommand(
                request.name,
                request.ownedQuantity,
                request.unitWeight?.grams, request.category, request.equipmentType,
                request.variant, request.unitLabel, request.weightScope
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse(response))
    }

    @PostMapping("/personal-equipment:from-catalog")
    fun adoptCatalog(
        authentication: Authentication?,
        @RequestBody request: AdoptCatalogEquipmentRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<DataResponse<PersonalEquipmentMutationResponse>> {
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        val accountId = authentication.accountPrincipal().userId
        val key = equipmentIdempotencyKey(servletRequest)
        val catalogId = request.catalogEquipmentId?.takeUnless { it.isBlank() }
            ?: throw ApiContractException.invalidRequest("必须指定 catalogEquipmentId")
        val quantity = request.ownedQuantity ?: throw ApiContractException.invalidRequest("必须指定 ownedQuantity")
        if (request.hasName && request.name == null) throw ApiContractException.invalidRequest("name 不能为 null")
        val result = service.adoptCatalogEquipment(accountId, key, AdoptCatalogEquipmentCommand(
            catalogId, quantity, request.name, request.unitWeight?.grams,
            request.variant, request.unitLabel, request.weightScope,
            request.hasUnitWeight, request.hasVariant, request.hasUnitLabel, request.hasWeightScope
        ))
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse(result))
    }

    @GetMapping("/personal-equipment/{personalEquipmentId}")
    fun detail(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getEquipment(authentication.accountPrincipal().userId, personalEquipmentId))
    }

    @PatchMapping("/personal-equipment/{personalEquipmentId}")
    fun update(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        @RequestBody request: UpdatePersonalEquipmentRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentMutationResponse> {
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        if (!request.hasName && !request.hasOwnedQuantity && !request.hasUnitWeight && !request.hasCategory &&
            !request.hasEquipmentType && !request.hasVariant && !request.hasUnitLabel && !request.hasWeightScope) {
            throw ApiContractException.unprocessable("validation_failed", "至少提供一个可修改字段")
        }
        if (request.hasName && request.name == null || request.hasOwnedQuantity && request.ownedQuantity == null) {
            throw ApiContractException.invalidRequest("name 和 ownedQuantity 不能为 null")
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
                    clearUnitWeight = request.hasUnitWeight && request.unitWeight == null,
                    category = request.category, equipmentType = request.equipmentType,
                    variant = request.variant, unitLabel = request.unitLabel, weightScope = request.weightScope,
                    hasCategory = request.hasCategory, hasEquipmentType = request.hasEquipmentType,
                    hasVariant = request.hasVariant, hasUnitLabel = request.hasUnitLabel, hasWeightScope = request.hasWeightScope
                )
            )
        )
    }

    @GetMapping("/personal-equipment/{personalEquipmentId}/deletion-impact")
    fun deletionImpact(
        authentication: Authentication?,
        @PathVariable personalEquipmentId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<PersonalEquipmentDeletionImpact> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getDeletionImpact(authentication.accountPrincipal().userId, personalEquipmentId))
    }

    @DeleteMapping("/personal-equipment/{personalEquipmentId}")
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

internal fun equipmentIdempotencyKey(request: HttpServletRequest): String {
    val values = request.getHeaders("Idempotency-Key")?.toList().orEmpty()
    if (values.size != 1 || values[0].isBlank()) throw ApiContractException.invalidRequest("必须提供唯一非空 Idempotency-Key")
    return values[0]
}
