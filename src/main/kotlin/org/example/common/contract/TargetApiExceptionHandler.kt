package org.example.common.contract

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.example.account.controller.AccountAuthController
import org.example.account.controller.AccountController
import org.example.equipment.controller.PersonalEquipmentController
import org.example.equipment.catalog.controller.EquipmentCatalogController
import org.example.equipment.controller.UserEquipmentListController
import org.example.route.controller.PublicRouteController
import org.example.trip.personal.controller.PersonalTripController
import org.example.trip.personal.controller.TripEquipmentController
import org.example.trip.personal.controller.TripGenerationContextController
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice(
    assignableTypes = [
        AccountAuthController::class,
        AccountController::class,
        PublicRouteController::class,
        PersonalEquipmentController::class,
        EquipmentCatalogController::class,
        UserEquipmentListController::class,
        PersonalTripController::class,
        TripGenerationContextController::class,
        TripEquipmentController::class
    ]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class TargetApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(TargetApiExceptionHandler::class.java)

    @ExceptionHandler(ApiContractException::class)
    fun handleContractException(exception: ApiContractException): ResponseEntity<ErrorResponse> =
        error(exception.status, exception.code, exception.message, exception.retryable, exception.details)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.BAD_REQUEST, "invalid_request", "请求体结构无效")

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class)
    fun handleValidationFailure(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNPROCESSABLE_ENTITY, "validation_failed", "请求参数校验失败")

    @ExceptionHandler(MissingServletRequestParameterException::class, MethodArgumentTypeMismatchException::class)
    fun handleInvalidParameter(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.BAD_REQUEST, "invalid_request", "请求参数无效")

    @ExceptionHandler(NoHandlerFoundException::class, NoResourceFoundException::class)
    fun handleNotFound(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.NOT_FOUND, "resource_not_found", "资源不存在")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "请求方法不受支持")

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMedia(
        exception: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val avatarRequest = request.requestURI == "/api/v1/account/profile/avatar-media"
        return if (avatarRequest) {
            error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "avatar_media_type_unsupported", "不支持该头像媒体类型")
        } else {
            error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "media_type_unsupported", "请求媒体类型不受支持")
        }
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataFailure(exception: DataAccessException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("Target API data access failure for ${request.method} ${request.requestURI}", exception)
        val path = request.requestURI.removePrefix(request.contextPath)
        val (code, message) = when {
            isEquipmentCatalogRequest(request) ->
                "equipment_catalog_unavailable" to "装备目录暂不可用"
            path.startsWith("/api/v1/personal-equipment") ||
                path.startsWith("/api/v1/equipment-lists") ->
                "personal_equipment_unavailable" to "个人装备服务暂不可用"
            path.startsWith("/api/v1/trips/") && path.contains("/equipment") ->
                "trip_equipment_unavailable" to "本次装备服务暂不可用"
            path.startsWith("/api/v1/public-routes") ->
                "public_route_read_failed" to "公共路线资料暂时无法读取"
            path.startsWith("/api/v1/trips") ->
                "personal_trip_unavailable" to "个人行程服务暂不可用"
            else -> "account_unavailable" to "账号服务暂不可用"
        }
        return error(HttpStatus.SERVICE_UNAVAILABLE, code, message, retryable = true)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("Target API internal failure for ${request.method} ${request.requestURI}", exception)
        if (isEquipmentCatalogRequest(request)) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "equipment_catalog_unavailable", "装备目录暂不可用", retryable = true)
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "服务内部错误")
    }

    private fun isEquipmentCatalogRequest(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return path == "/api/v1/equipment-catalog" || path.startsWith("/api/v1/equipment-catalog/")
    }

    private fun error(
        status: HttpStatus,
        code: String,
        message: String,
        retryable: Boolean = false,
        details: Any? = null
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(
        ErrorResponse(ApiError(code, message, retryable, details))
    )
}
