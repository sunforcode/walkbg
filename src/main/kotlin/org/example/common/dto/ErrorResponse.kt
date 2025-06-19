package org.example.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * 扩展的API响应格式，用于错误响应
 * 与ApiResponse保持一致的结构，但增加了错误特有的字段
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorApiResponse(
    val success: Boolean = false,
    val message: String,
    val code: Int, // 与ApiResponse保持一致，使用code而不是httpStatus
    val data: ErrorData? = null, // 使用data字段包装错误详情，保持与ApiResponse结构一致
    @JsonProperty("timestamp")
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun create(
            message: String,
            errorCode: String,
            httpStatus: Int,
            details: Map<String, Any>? = null,
            path: String? = null,
            traceId: String? = null
        ): ErrorApiResponse {
            val errorData = ErrorData(
                errorCode = errorCode,
                details = details,
                path = path,
                traceId = traceId
            )

            return ErrorApiResponse(
                message = message,
                code = httpStatus,
                data = errorData
            )
        }

        // 简化版本，只包含基本错误信息
        fun simple(
            message: String,
            httpStatus: Int,
            errorCode: String = "BUSINESS_ERROR"
        ): ErrorApiResponse {
            return ErrorApiResponse(
                message = message,
                code = httpStatus,
                data = ErrorData(errorCode = errorCode)
            )
        }
    }
}

/**
 * 错误详情数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorData(
    val errorCode: String,
    val details: Map<String, Any>? = null,
    val path: String? = null,
    val traceId: String? = null
)

/**
 * 验证错误详情
 */
data class ValidationError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)

/**
 * 验证错误响应，使用与ApiResponse一致的结构
 */
data class ValidationErrorResponse(
    val success: Boolean = false,
    val message: String = "请求参数验证失败",
    val code: Int = 400, // 与ApiResponse保持一致
    val data: ValidationErrorData,
    @JsonProperty("timestamp")
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun create(
            errors: List<ValidationError>,
            path: String? = null,
            traceId: String? = null,
            message: String = "请求参数验证失败"
        ): ValidationErrorResponse {
            return ValidationErrorResponse(
                message = message,
                data = ValidationErrorData(
                    errorCode = "VALIDATION_ERROR",
                    errors = errors,
                    path = path,
                    traceId = traceId
                )
            )
        }
    }
}

/**
 * 验证错误数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ValidationErrorData(
    val errorCode: String = "VALIDATION_ERROR",
    val errors: List<ValidationError>,
    val path: String? = null,
    val traceId: String? = null
)
