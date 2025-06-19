package org.example.common.exception

import org.example.common.dto.ApiResponse
import org.example.common.dto.ErrorApiResponse
import org.example.common.dto.ErrorData
import org.example.common.dto.ValidationError
import org.example.common.dto.ValidationErrorResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
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
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import java.util.*

/**
 * 全局异常处理器
 * 统一处理应用中的异常，返回标准化的错误响应
 *
 * 特性：
 * - 统一的错误响应格式
 * - 完善的日志记录
 * - 请求追踪支持
 * - 敏感信息保护
 * - 多种异常类型支持
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * 生成追踪ID
     */
    private fun generateTraceId(): String {
        return MDC.get("traceId") ?: UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    /**
     * 记录异常日志
     * 对于预期的业务异常，不打印堆栈信息以避免日志污染
     */
    private fun logException(level: String, message: String, ex: Throwable?, request: HttpServletRequest, includeStackTrace: Boolean = true) {
        val traceId = generateTraceId()
        val logMessage = "[$traceId] ${request.method} ${request.requestURI} - $message"

        when (level.uppercase()) {
            "ERROR" -> {
                if (includeStackTrace && ex != null) {
                    logger.error(logMessage, ex)
                } else {
                    logger.error(logMessage)
                }
            }
            "WARN" -> {
                if (includeStackTrace && ex != null) {
                    logger.warn(logMessage, ex)
                } else {
                    logger.warn(logMessage)
                }
            }
            "INFO" -> logger.info(logMessage)
            else -> logger.debug(logMessage)
        }
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "业务异常: ${ex.message}", ex, request, includeStackTrace = false)

        val errorData = ErrorData(
            errorCode = ex.errorCode,
            details = ex.details,
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = ex.message ?: "业务处理失败",
            data = errorData,
            code = ex.httpStatus.value()
        )

        return ResponseEntity.status(ex.httpStatus).body(response)
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val traceId = generateTraceId()
        logException("WARN", "参数验证失败", ex, request)

        val errors = ex.bindingResult.fieldErrors.map { fieldError ->
            ValidationError(
                field = fieldError.field,
                rejectedValue = fieldError.rejectedValue,
                message = fieldError.defaultMessage ?: "验证失败"
            )
        }

        val errorResponse = ValidationErrorResponse.create(
            errors = errors,
            path = request.requestURI,
            traceId = traceId
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val traceId = generateTraceId()
        logException("WARN", "约束验证失败", ex, request)

        val errors = ex.constraintViolations.map { violation ->
            ValidationError(
                field = violation.propertyPath.toString(),
                rejectedValue = violation.invalidValue,
                message = violation.message
            )
        }

        val errorResponse = ValidationErrorResponse.create(
            errors = errors,
            path = request.requestURI,
            traceId = traceId
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "参数类型错误", ex, request)

        val message = "参数类型错误: ${ex.name} 应该是 ${ex.requiredType?.simpleName} 类型"
        val details = mapOf(
            "parameter" to ex.name,
            "expectedType" to (ex.requiredType?.simpleName ?: "unknown"),
            "actualValue" to (ex.value?.toString() ?: "null")
        )

        val errorData = ErrorData(
            errorCode = "PARAMETER_TYPE_MISMATCH",
            details = details,
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = message,
            data = errorData,
            code = 400
        )

        return ResponseEntity.badRequest().body(response)
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "缺少必需参数", ex, request)

        val message = "缺少必需参数: ${ex.parameterName}"
        val details = mapOf(
            "parameter" to ex.parameterName,
            "type" to ex.parameterType
        )

        val errorData = ErrorData(
            errorCode = "MISSING_PARAMETER",
            details = details,
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = message,
            data = errorData,
            code = 400
        )

        return ResponseEntity.badRequest().body(response)
    }

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "JSON解析失败", ex, request)

        val errorData = ErrorData(
            errorCode = "JSON_PARSE_ERROR",
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = "请求体格式错误，请检查JSON格式",
            data = errorData,
            code = 400
        )

        return ResponseEntity.badRequest().body(response)
    }

    /**
     * 处理不支持的媒体类型异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(
        ex: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "不支持的媒体类型", ex, request)

        val supportedTypes = ex.supportedMediaTypes.joinToString(", ")
        val message = "不支持的媒体类型: ${ex.contentType}，支持的类型: $supportedTypes"

        val errorData = ErrorData(
            errorCode = "UNSUPPORTED_MEDIA_TYPE",
            details = mapOf("supportedTypes" to supportedTypes),
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = message,
            data = errorData,
            code = 415
        )

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response)
    }

    /**
     * 处理404错误 - 未找到处理器
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "API接口不存在", ex, request, includeStackTrace = false)

        val message = "API接口不存在: ${ex.httpMethod} ${ex.requestURL}"
        val errorData = ErrorData(
            errorCode = "API_NOT_FOUND",
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = message,
            data = errorData,
            code = 404
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * 处理404错误 - 未找到资源 (Spring Boot 3.x)
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(
        ex: NoResourceFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "资源不存在", ex, request, includeStackTrace = false)

        val message = "API接口不存在: ${request.method} ${request.requestURI}"
        val errorData = ErrorData(
            errorCode = "RESOURCE_NOT_FOUND",
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = message,
            data = errorData,
            code = 404
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * 处理405错误 - 方法不允许
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "请求方法不支持", ex, request, includeStackTrace = false)

        val supportedMethods = ex.supportedMethods?.joinToString(", ") ?: "无"
        val message = "请求方法不支持: ${ex.method}，支持的方法: $supportedMethods"

        val errorData = ErrorData(
            errorCode = "METHOD_NOT_ALLOWED",
            details = mapOf("supportedMethods" to supportedMethods),
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = message,
            data = errorData,
            code = 405
        )

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response)
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "权限不足", ex, request)

        val errorData = ErrorData(
            errorCode = "ACCESS_DENIED",
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = "权限不足，无法访问该资源",
            data = errorData,
            code = 403
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    /**
     * 处理数据完整性违反异常
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("WARN", "数据完整性违反", ex, request)

        val errorData = ErrorData(
            errorCode = "DATA_INTEGRITY_VIOLATION",
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = "数据操作失败，可能存在重复数据或违反约束条件",
            data = errorData,
            code = 409
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<ErrorData>> {
        val traceId = generateTraceId()
        logException("ERROR", "系统内部错误", ex, request)

        val errorData = ErrorData(
            errorCode = "INTERNAL_SERVER_ERROR",
            path = request.requestURI,
            traceId = traceId
        )

        val response = ApiResponse(
            success = false,
            message = "系统内部错误，请稍后重试",
            data = errorData,
            code = 500
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
