package org.example.scene

import com.fasterxml.jackson.databind.JsonNode
import jakarta.servlet.http.HttpServletRequest
import org.example.account.controller.accountPrincipal
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class SceneController(private val service: SceneApplicationService, private val configuration: SceneConfiguration, private val domain: SceneDomainService) {
    @PostMapping("/scene-management/media", consumes = ["image/png", "image/jpeg"])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(request: HttpServletRequest): DataResponse<SceneImage> {
        authorizeManagement(request)
        if (request.contentLengthLong > SceneDomainService.MAX_BYTES) throw domain.tooLarge()
        val bytes = request.inputStream.readNBytes(SceneDomainService.MAX_BYTES + 1)
        if (bytes.size > SceneDomainService.MAX_BYTES) throw domain.tooLarge()
        val type = MediaType.parseMediaType(request.contentType).let { "${it.type}/${it.subtype}" }
        return DataResponse(service.upload(bytes, type))
    }

    @GetMapping("/scene-media/{mediaId}")
    fun media(@PathVariable mediaId: String, authentication: Authentication?, request: HttpServletRequest): ResponseEntity<ByteArray> {
        rejectQuery(request)
        if (request.getHeader("Authorization") != null) authentication.accountPrincipal()
        val media = service.readMedia(mediaId)
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(media.contentType)
            eTag = "\"${media.mediaId}\""
            cacheControl = "public, max-age=31536000, immutable"
            set("X-Content-Type-Options", "nosniff")
        }
        val requestedTags = request.getHeader(HttpHeaders.IF_NONE_MATCH)?.split(',')?.map { it.trim().removePrefix("W/") } ?: emptyList()
        if (headers.eTag in requestedTags || "*" in requestedTags) return ResponseEntity(null, headers, HttpStatus.NOT_MODIFIED)
        headers.contentLength = media.bytes.size.toLong()
        return ResponseEntity(media.bytes, headers, HttpStatus.OK)
    }

    @GetMapping("/public-routes/{routeId}/scenes")
    fun publicRoute(@PathVariable routeId: String, @RequestParam routeVersionId: String, authentication: Authentication?, request: HttpServletRequest): DataResponse<RouteSceneProjection> {
        rejectQuery(request, setOf("routeVersionId"))
        if (request.getHeader("Authorization") != null) authentication.accountPrincipal()
        if (routeVersionId.isBlank()) throw ApiContractException.invalidRequest("routeVersionId 必填")
        return DataResponse(service.publicRoute(routeId, routeVersionId))
    }

    @GetMapping("/trips/{tripId}/scenes")
    fun trip(@PathVariable tripId: String, authentication: Authentication?, request: HttpServletRequest): DataResponse<TripSceneProjection> {
        rejectQuery(request)
        return DataResponse(service.personalTrip(authentication.accountPrincipal().userId, tripId))
    }

    @GetMapping("/scene-management/routes/{routeId}/versions/{versionId}")
    fun managedRoute(@PathVariable routeId: String, @PathVariable versionId: String, request: HttpServletRequest): DataResponse<ManagedSceneProjection<RouteSceneProjection>> {
        authorizeManagement(request)
        return DataResponse(service.managedRoute(routeId, versionId))
    }

    @PutMapping("/scene-management/routes/{routeId}/versions/{versionId}")
    fun replaceRoute(@PathVariable routeId: String, @PathVariable versionId: String, @RequestBody body: JsonNode, request: HttpServletRequest): DataResponse<ManagedSceneProjection<RouteSceneProjection>> {
        authorizeManagement(request)
        exactFields(body, setOf("expectedRevision", "overviewMediaId", "days"))
        return DataResponse(service.replaceRoute(routeId, versionId, ReplaceRouteScenes(revision(body), bindings(body, "referenceDayId"))))
    }

    @GetMapping("/scene-management/trips/{tripId}")
    fun managedTrip(@PathVariable tripId: String, request: HttpServletRequest): DataResponse<ManagedSceneProjection<TripSceneProjection>> {
        authorizeManagement(request)
        return DataResponse(service.managedTrip(tripId))
    }

    @PutMapping("/scene-management/trips/{tripId}")
    fun replaceTrip(@PathVariable tripId: String, @RequestBody body: JsonNode, request: HttpServletRequest): DataResponse<ManagedSceneProjection<TripSceneProjection>> {
        authorizeManagement(request)
        exactFields(body, setOf("expectedRevision", "tripRevision", "routeVersionId", "overviewMediaId", "days"))
        return DataResponse(service.replaceTrip(tripId, ReplaceTripScenes(revision(body), requiredText(body, "tripRevision"), requiredText(body, "routeVersionId"), bindings(body, "tripDayId"))))
    }

    private fun authorizeManagement(request: HttpServletRequest) {
        val tokens = request.getHeaders("X-Scene-Management-Token").toList()
        configuration.authorize(tokens.singleOrNull())
        rejectQuery(request)
    }
    private fun rejectQuery(request: HttpServletRequest, allowed: Set<String> = emptySet()) {
        if (request.parameterMap.any { (key, values) -> key !in allowed || values.size != 1 }) throw ApiContractException.invalidRequest("请求包含未定义或重复查询参数")
    }
    private fun exactFields(node: JsonNode, allowed: Set<String>) {
        if (!node.isObject || node.fieldNames().asSequence().any { it !in allowed }) throw ApiContractException.invalidRequest("请求包含未定义字段或结构无效")
    }
    private fun requiredText(node: JsonNode, name: String): String {
        val value = node.path(name)
        if (!value.isTextual || value.asText().isBlank()) throw ApiContractException.invalidRequest("$name 必须是非空字符串")
        return value.asText()
    }
    private fun revision(node: JsonNode): Long {
        val value = node.path("expectedRevision")
        if (!value.isIntegralNumber || !value.canConvertToLong() || value.asLong() < 0 || value.asLong() == Long.MAX_VALUE) throw ApiContractException.invalidRequest("expectedRevision 必须是非负整数")
        return value.asLong()
    }
    private fun bindings(node: JsonNode, dayIdField: String): SceneBindings {
        val overview = node.get("overviewMediaId")?.let { requiredText(node, "overviewMediaId") }
        val list = node.path("days")
        if (!list.isArray) throw ApiContractException.invalidRequest("days 必须是数组")
        return SceneBindings(overview, list.map { item ->
            exactFields(item, setOf(dayIdField, "mediaId"))
            SceneDayBinding(requiredText(item, dayIdField), requiredText(item, "mediaId"))
        })
    }
}
