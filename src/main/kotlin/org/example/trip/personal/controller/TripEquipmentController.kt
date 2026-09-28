package org.example.trip.personal.controller

import com.fasterxml.jackson.databind.JsonNode
import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.equipment.catalog.model.CatalogType
import org.example.trip.personal.dto.*
import org.example.trip.personal.service.TripEquipmentApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/trips")
class TripEquipmentController(private val service: TripEquipmentApplicationService) {
    @GetMapping("/{tripId}/equipment")
    fun get(authentication: Authentication?, @PathVariable tripId: String, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request)
        return result(service.getEquipment(authentication.accountPrincipal().userId, tripId))
    }

    @GetMapping("/{tripId}/equipment/preparation")
    fun preparation(authentication: Authentication?, @PathVariable tripId: String, request: HttpServletRequest): ResponseEntity<DataResponse<TripPreparationProjection>> {
        rejectQuery(request)
        val projection = service.preparation(authentication.accountPrincipal().userId, tripId)
        return ResponseEntity.ok().eTag("\"${projection.revision}\"").body(DataResponse(projection))
    }

    @PostMapping("/{tripId}/equipment/items")
    fun create(authentication: Authentication?, @PathVariable tripId: String, @RequestBody body: JsonNode, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request)
        fields(body, ITEM_FIELDS)
        body.fields().forEachRemaining { (_, value) -> if (value.isNull) invalid("新增字段不能为 null") }
        val expectedRevision = revision(request, CONFIG_FIELDS.any(body::has))
        return result(service.createItem(authentication.accountPrincipal().userId, tripId,
            CreateTripEquipmentItemCommand(text(body, "name", true), integer(body, "quantity", true), weight(body), text(body, "note"), configuration(body)), expectedRevision), 201)
    }

    @PatchMapping("/{tripId}/equipment/items/{itemId}")
    fun update(authentication: Authentication?, @PathVariable tripId: String, @PathVariable itemId: String, @RequestBody body: JsonNode, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request)
        fields(body, ITEM_FIELDS)
        if (body.isEmpty) throw ApiContractException.unprocessable("validation_failed", "至少提供一个可修改字段")
        val expectedRevision = revision(request, CONFIG_FIELDS.any(body::has))
        val command = UpdateTripEquipmentItemCommand(
            text(body, "name", body.has("name")), integer(body, "quantity", body.has("quantity")), weight(body), text(body, "note"),
            body.has("name"), body.has("quantity"), body.has("unitWeight"), body.has("note"), configuration(body), CONFIG_FIELDS.filter(body::has).toSet()
        )
        return result(service.updateItem(authentication.accountPrincipal().userId, tripId, itemId, command, expectedRevision))
    }

    @DeleteMapping("/{tripId}/equipment/items/{itemId}")
    fun delete(authentication: Authentication?, @PathVariable tripId: String, @PathVariable itemId: String, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request); emptyBody(request)
        return result(service.deleteItem(authentication.accountPrincipal().userId, tripId, itemId, revision(request, false)))
    }

    @PostMapping("/{tripId}/equipment:recheck-ownership")
    fun recheck(authentication: Authentication?, @PathVariable tripId: String, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request); emptyBody(request)
        return result(service.recheckOwnership(authentication.accountPrincipal().userId, tripId, revision(request, false)))
    }

    @PostMapping("/{tripId}/equipment/items:from-personal")
    fun fromPersonal(authentication: Authentication?, @PathVariable tripId: String, @RequestBody body: JsonNode, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request)
        val command = selection(body)
        return result(service.fromPersonal(authentication.accountPrincipal().userId, tripId, command, requireNotNull(revision(request, true)), requiredHeader(request, "Idempotency-Key")), 201)
    }

    @PostMapping("/{tripId}/equipment:append-list")
    fun appendList(authentication: Authentication?, @PathVariable tripId: String, @RequestBody body: JsonNode, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request); fields(body, setOf("equipmentListId"))
        return result(service.appendList(authentication.accountPrincipal().userId, tripId,
            AppendEquipmentListCommand(identity(body, "equipmentListId")), requireNotNull(revision(request, true)), requiredHeader(request, "Idempotency-Key")))
    }

    @PutMapping("/{tripId}/equipment/items/{itemId}/personal-equipment")
    fun replace(authentication: Authentication?, @PathVariable tripId: String, @PathVariable itemId: String, @RequestBody body: JsonNode, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request)
        return result(service.replacePersonal(authentication.accountPrincipal().userId, tripId, itemId, selection(body), requireNotNull(revision(request, true))))
    }

    @PutMapping("/{tripId}/equipment/items/{itemId}/packing")
    fun packing(authentication: Authentication?, @PathVariable tripId: String, @PathVariable itemId: String, @RequestBody body: JsonNode, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request); fields(body, setOf("packedQuantity"))
        return result(service.setPacking(authentication.accountPrincipal().userId, tripId, itemId, requireNotNull(integer(body, "packedQuantity", true)), requireNotNull(revision(request, true))))
    }

    @PostMapping("/{tripId}/equipment:reset-packing")
    fun resetPacking(authentication: Authentication?, @PathVariable tripId: String, request: HttpServletRequest): ResponseEntity<DataResponse<TripEquipmentProjection>> {
        rejectQuery(request); emptyBody(request)
        return result(service.resetPacking(authentication.accountPrincipal().userId, tripId, requireNotNull(revision(request, true))))
    }

    private fun selection(body: JsonNode): SelectPersonalEquipmentCommand {
        fields(body, setOf("personalEquipmentId", "quantity"))
        return SelectPersonalEquipmentCommand(identity(body, "personalEquipmentId"), requireNotNull(integer(body, "quantity", true)))
    }

    private fun identity(body: JsonNode, field: String): String = requireNotNull(text(body, field, true)).also {
        if (it.isBlank()) invalid("$field 必须是非空身份")
    }

    private fun configuration(body: JsonNode): TripItemConfiguration {
        val category = enum(body, "category", CatalogType.categories)
        val type = enum(body, "equipmentType", CatalogType.entries.map { it.code }.toSet())
        val allocation = body.get("carryAllocation")?.takeUnless { it.isNull }?.let {
            fields(it, setOf("backpackQuantity", "wornQuantity", "handheldQuantity"))
            TripCarryAllocation(requireNotNull(integer(it, "backpackQuantity", true)), requireNotNull(integer(it, "wornQuantity", true)), requireNotNull(integer(it, "handheldQuantity", true)))
        }
        val supply = body.get("supply")?.takeUnless { it.isNull }?.let {
            fields(it, setOf("kind", "amount", "containerMode"))
            val amount = it.get("amount") ?: invalid("补给必须有 amount")
            fields(amount, setOf("value", "unit"))
            TripSupply(
                requireNotNull(enum(it, "kind", setOf("food", "drinking_water", "fuel"), true)),
                TripSupplyAmount(number(amount, "value"), requireNotNull(enum(amount, "unit", setOf("gram", "milliliter", "portion"), true))),
                requireNotNull(enum(it, "containerMode", setOf("included", "excluded"), true))
            )
        }
        return TripItemConfiguration(category, type, text(body, "variant"), enum(body, "unitLabel", CatalogType.unitLabels), text(body, "weightScope"), allocation, supply)
    }

    private fun weight(body: JsonNode): Double? = body.get("unitWeight")?.takeUnless { it.isNull }?.let {
        fields(it, setOf("grams")); number(it, "grams")
    }

    private fun fields(node: JsonNode, allowed: Set<String>) {
        if (!node.isObject || node.fieldNames().asSequence().any { it !in allowed }) invalid("请求包含未定义字段或对象类型错误")
    }
    private fun text(node: JsonNode, field: String, required: Boolean = false): String? {
        val value = node.get(field)
        if (value == null || value.isNull) {
            if (required) invalid("$field 必须提供且不能为 null")
            return null
        }
        if (!value.isTextual) invalid("$field 必须为字符串")
        return value.textValue()
    }
    private fun enum(node: JsonNode, field: String, allowed: Set<String>, required: Boolean = false): String? =
        text(node, field, required)?.also { if (it !in allowed) invalid("$field 的枚举值无效") }
    private fun integer(node: JsonNode, field: String, required: Boolean = false): Int? {
        val value = node.get(field)
        if (value == null || value.isNull) {
            if (required) invalid("$field 必须提供且不能为 null")
            return null
        }
        if (!value.isIntegralNumber || !value.canConvertToInt()) invalid("$field 必须为整数")
        return value.intValue()
    }
    private fun number(node: JsonNode, field: String): Double {
        val value = node.get(field) ?: invalid("$field 必须提供")
        if (!value.isNumber || !value.doubleValue().isFinite()) invalid("$field 必须为有限数值")
        return value.doubleValue()
    }
    private fun rejectQuery(request: HttpServletRequest) { if (request.parameterMap.isNotEmpty()) invalid("请求包含未定义查询参数") }
    private fun emptyBody(request: HttpServletRequest) { if (request.inputStream.read() != -1) invalid("本任务只接受零字节空请求体") }
    private fun requiredHeader(request: HttpServletRequest, name: String): String {
        val values = request.getHeaders(name).toList()
        if (values.size != 1 || values.single().isBlank()) invalid("$name 必须提供且只能出现一次")
        return values.single()
    }
    private fun revision(request: HttpServletRequest, required: Boolean): String? {
        val values = request.getHeaders("If-Match").toList()
        if (values.isEmpty() && !required) return null
        val value = requiredHeader(request, "If-Match")
        if (!value.matches(Regex("\"[^\"\\s,]+\""))) invalid("If-Match 必须是单个非空强标签")
        return value.substring(1, value.length - 1)
    }
    private fun result(value: TripEquipmentProjection, status: Int = 200): ResponseEntity<DataResponse<TripEquipmentProjection>> =
        ResponseEntity.status(status).eTag("\"${value.revision}\"").body(DataResponse(value))
    private fun invalid(message: String): Nothing = throw ApiContractException.invalidRequest(message)

    companion object {
        private val CONFIG_FIELDS = setOf("category", "equipmentType", "variant", "unitLabel", "weightScope", "carryAllocation", "supply")
        private val ITEM_FIELDS = setOf("name", "quantity", "unitWeight", "note") + CONFIG_FIELDS
    }
}
