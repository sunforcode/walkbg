package org.example.common.exception

import org.springframework.http.HttpStatus

/**
 * 业务异常基类
 * 支持错误码、HTTP状态码和详细错误信息
 */
class BusinessException(
    message: String,
    val errorCode: String = "BUSINESS_ERROR",
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    val details: Map<String, Any>? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    companion object {
        // 400 Bad Request
        fun badRequest(message: String, errorCode: String = "BAD_REQUEST", details: Map<String, Any>? = null): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.BAD_REQUEST, details)
        }

        // 401 Unauthorized
        fun unauthorized(message: String = "认证失败", errorCode: String = "UNAUTHORIZED"): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.UNAUTHORIZED)
        }

        // 403 Forbidden
        fun forbidden(message: String = "权限不足", errorCode: String = "FORBIDDEN"): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.FORBIDDEN)
        }

        // 404 Not Found
        fun notFound(message: String, errorCode: String = "NOT_FOUND"): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.NOT_FOUND)
        }

        // 409 Conflict
        fun conflict(message: String, errorCode: String = "CONFLICT", details: Map<String, Any>? = null): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.CONFLICT, details)
        }

        // 422 Unprocessable Entity
        fun unprocessableEntity(message: String, errorCode: String = "UNPROCESSABLE_ENTITY", details: Map<String, Any>? = null): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.UNPROCESSABLE_ENTITY, details)
        }

        // 429 Too Many Requests
        fun tooManyRequests(message: String = "请求过于频繁", errorCode: String = "TOO_MANY_REQUESTS"): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.TOO_MANY_REQUESTS)
        }

        // 500 Internal Server Error
        fun internalError(message: String = "系统内部错误", errorCode: String = "INTERNAL_ERROR", cause: Throwable? = null): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.INTERNAL_SERVER_ERROR, null, cause)
        }

        // 503 Service Unavailable
        fun serviceUnavailable(message: String = "服务暂不可用", errorCode: String = "SERVICE_UNAVAILABLE"): BusinessException {
            return BusinessException(message, errorCode, HttpStatus.SERVICE_UNAVAILABLE)
        }

        // 业务特定异常
        fun validation(message: String, field: String? = null): BusinessException {
            val details = field?.let { mapOf("field" to it) }
            return BusinessException(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, details)
        }

        fun duplicate(resource: String, field: String, value: String): BusinessException {
            val details = mapOf("resource" to resource, "field" to field, "value" to value)
            return BusinessException("${resource}已存在：${field} = ${value}", "DUPLICATE_ERROR", HttpStatus.CONFLICT, details)
        }

        fun resourceNotFound(resource: String, id: String): BusinessException {
            val details = mapOf("resource" to resource, "id" to id)
            return BusinessException("${resource}不存在：${id}", "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, details)
        }
    }
}
