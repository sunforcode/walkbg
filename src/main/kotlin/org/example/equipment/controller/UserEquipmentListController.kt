package org.example.equipment.controller

import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.equipment.dto.EquipmentListCollectionResponse
import org.example.equipment.dto.EquipmentListCreateResponse
import org.example.equipment.dto.EquipmentListDeletionResponse
import org.example.equipment.dto.EquipmentListDetailProjection
import org.example.equipment.dto.EquipmentListMemberRequest
import org.example.equipment.dto.EquipmentListMutationResponse
import org.example.equipment.dto.EquipmentListNameRequest
import org.example.equipment.service.PersonalEquipmentApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/equipment-lists")
class UserEquipmentListController(
    private val service: PersonalEquipmentApplicationService
) {
    @GetMapping
    fun collection(
        authentication: Authentication?,
        servletRequest: HttpServletRequest
    ): DataResponse<EquipmentListCollectionResponse> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getEquipmentLists(authentication.accountPrincipal().userId))
    }

    @PostMapping
    fun create(
        authentication: Authentication?,
        @RequestBody request: EquipmentListNameRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<DataResponse<EquipmentListCreateResponse>> {
        rejectUnknownQuery(servletRequest)
        validateNameRequest(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            DataResponse(service.createEquipmentList(authentication.accountPrincipal().userId, request.name))
        )
    }

    @GetMapping("/{equipmentListId}")
    fun detail(
        authentication: Authentication?,
        @PathVariable equipmentListId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<EquipmentListDetailProjection> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.getEquipmentList(authentication.accountPrincipal().userId, equipmentListId))
    }

    @PatchMapping("/{equipmentListId}")
    fun rename(
        authentication: Authentication?,
        @PathVariable equipmentListId: String,
        @RequestBody request: EquipmentListNameRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<EquipmentListMutationResponse> {
        rejectUnknownQuery(servletRequest)
        validateNameRequest(request)
        return DataResponse(
            service.renameEquipmentList(authentication.accountPrincipal().userId, equipmentListId, request.name)
        )
    }

    @DeleteMapping("/{equipmentListId}")
    fun delete(
        authentication: Authentication?,
        @PathVariable equipmentListId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<EquipmentListDeletionResponse> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(service.deleteEquipmentList(authentication.accountPrincipal().userId, equipmentListId))
    }

    @PutMapping("/{equipmentListId}/members/{personalEquipmentId}")
    fun putMember(
        authentication: Authentication?,
        @PathVariable equipmentListId: String,
        @PathVariable personalEquipmentId: String,
        @RequestBody request: EquipmentListMemberRequest,
        servletRequest: HttpServletRequest
    ): DataResponse<EquipmentListMutationResponse> {
        rejectUnknownQuery(servletRequest)
        rejectUnknownFields(request)
        if (!request.hasQuantity || request.quantity == null) {
            throw ApiContractException.invalidRequest("成员请求必须包含 quantity")
        }
        return DataResponse(
            service.putListMember(
                authentication.accountPrincipal().userId,
                equipmentListId,
                personalEquipmentId,
                request.quantity
            )
        )
    }

    @DeleteMapping("/{equipmentListId}/members/{personalEquipmentId}")
    fun deleteMember(
        authentication: Authentication?,
        @PathVariable equipmentListId: String,
        @PathVariable personalEquipmentId: String,
        servletRequest: HttpServletRequest
    ): DataResponse<EquipmentListMutationResponse> {
        rejectUnknownQuery(servletRequest)
        return DataResponse(
            service.removeListMember(
                authentication.accountPrincipal().userId,
                equipmentListId,
                personalEquipmentId
            )
        )
    }

    private fun validateNameRequest(request: EquipmentListNameRequest) {
        rejectUnknownFields(request)
        if (!request.hasName || request.name == null) {
            throw ApiContractException.invalidRequest("请求必须包含 name")
        }
    }
}
