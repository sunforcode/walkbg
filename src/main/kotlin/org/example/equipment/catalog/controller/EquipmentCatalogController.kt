package org.example.equipment.catalog.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.common.contract.ErrorResponse
import org.example.equipment.catalog.dto.CatalogBrandCollection
import org.example.equipment.catalog.dto.CatalogEquipmentCollection
import org.example.equipment.catalog.dto.CatalogEquipmentDetailProjection
import org.example.equipment.catalog.model.CatalogFilter
import org.example.equipment.catalog.service.EquipmentCatalogApplicationService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/equipment-catalog")
@Tag(name = "Equipment Catalog", description = "共享装备与补给资料，只读")
@SecurityScheme(name = "equipmentCatalogSession", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "equipmentCatalogSession")
@ApiResponses(value = [
    ApiResponse(responseCode = "400", description = "invalid_request", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ApiResponse(responseCode = "401", description = "authentication_required", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ApiResponse(responseCode = "503", description = "equipment_catalog_unavailable，retryable=true", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
])
class EquipmentCatalogController(private val service: EquipmentCatalogApplicationService) {
    @GetMapping("/brands")
    @Operation(summary = "读取全部品牌", description = "不接受查询参数，按 identity 字符串升序返回完整集合")
    fun brands(@Parameter(hidden = true) authentication: Authentication?, @Parameter(hidden = true) request: HttpServletRequest): DataResponse<CatalogBrandCollection> {
        authentication.accountPrincipal()
        query(request, emptySet())
        return DataResponse(service.getBrands())
    }

    @GetMapping("/items")
    @Operation(summary = "筛选装备与补给资料", description = "条件全部 AND；数值为闭区间；等值与范围互斥；温标条件必须指定 sleeping_bag；完整返回，不分页")
    @ApiResponse(responseCode = "422", description = "validation_failed：非正重量或范围倒置", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @Parameters(value = [
        Parameter(name = "keyword", `in` = ParameterIn.QUERY, description = "名称、型号或品牌名称字面包含", schema = Schema(type = "string")),
        Parameter(name = "category", `in` = ParameterIn.QUERY, description = "精确分类 code", schema = Schema(type = "string")),
        Parameter(name = "brandId", `in` = ParameterIn.QUERY, description = "原样精确匹配的不透明品牌身份", schema = Schema(type = "string")),
        Parameter(name = "equipmentType", `in` = ParameterIn.QUERY, description = "精确类型 code", schema = Schema(type = "string")),
        Parameter(name = "weightGrams", `in` = ParameterIn.QUERY, description = "正克重等值", schema = Schema(type = "number", format = "double")),
        Parameter(name = "minWeightGrams", `in` = ParameterIn.QUERY, description = "正克重下界", schema = Schema(type = "number", format = "double")),
        Parameter(name = "maxWeightGrams", `in` = ParameterIn.QUERY, description = "正克重上界", schema = Schema(type = "number", format = "double")),
        Parameter(name = "comfortTemperatureCelsius", `in` = ParameterIn.QUERY, description = "舒适温标等值，摄氏度", schema = Schema(type = "number", format = "double")),
        Parameter(name = "minComfortTemperatureCelsius", `in` = ParameterIn.QUERY, description = "舒适温标下界，摄氏度", schema = Schema(type = "number", format = "double")),
        Parameter(name = "maxComfortTemperatureCelsius", `in` = ParameterIn.QUERY, description = "舒适温标上界，摄氏度", schema = Schema(type = "number", format = "double"))
    ])
    fun items(@Parameter(hidden = true) authentication: Authentication?, @Parameter(hidden = true) request: HttpServletRequest): DataResponse<CatalogEquipmentCollection> {
        authentication.accountPrincipal()
        val parameters = query(request, allowedFilters)
        val filter = CatalogFilter(
            keyword = parameters["keyword"], category = parameters["category"], brandId = parameters["brandId"], equipmentType = parameters["equipmentType"],
            weightGrams = number(parameters["weightGrams"]), minWeightGrams = number(parameters["minWeightGrams"]), maxWeightGrams = number(parameters["maxWeightGrams"]),
            comfortTemperatureCelsius = number(parameters["comfortTemperatureCelsius"]),
            minComfortTemperatureCelsius = number(parameters["minComfortTemperatureCelsius"]),
            maxComfortTemperatureCelsius = number(parameters["maxComfortTemperatureCelsius"])
        )
        return DataResponse(service.getItems(filter))
    }

    @GetMapping("/items/{equipmentId}")
    @Operation(summary = "读取装备资料详情", description = "只展开匹配装备类型的规格，不接受查询参数")
    @ApiResponse(responseCode = "404", description = "resource_not_found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun detail(@Parameter(hidden = true) authentication: Authentication?, @PathVariable equipmentId: String,
               @Parameter(hidden = true) request: HttpServletRequest): DataResponse<CatalogEquipmentDetailProjection> {
        authentication.accountPrincipal()
        query(request, emptySet())
        return DataResponse(service.getItem(equipmentId))
    }

    private fun query(request: HttpServletRequest, allowed: Set<String>): Map<String, String> {
        if (request.parameterMap.any { (name, values) -> name !in allowed || values.size != 1 || values[0].isBlank() }) {
            throw ApiContractException.invalidRequest("查询参数未知、重复或为空")
        }
        return request.parameterMap.mapValues { it.value[0] }
    }

    private fun number(value: String?): Double? {
        if (value == null) return null
        if (!decimal.matches(value)) throw ApiContractException.invalidRequest("数值参数格式无效")
        return value.toDoubleOrNull()?.takeIf { it.isFinite() }
            ?: throw ApiContractException.invalidRequest("数值参数必须为有限数")
    }

    companion object {
        private val decimal = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")
        private val allowedFilters = setOf("keyword", "category", "brandId", "equipmentType", "weightGrams", "minWeightGrams", "maxWeightGrams",
            "comfortTemperatureCelsius", "minComfortTemperatureCelsius", "maxComfortTemperatureCelsius")
    }
}
