package org.example.common.exception

import org.example.common.exception.BusinessException
import org.example.common.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import jakarta.validation.ConstraintViolationException

/**
 * 全局异常处理器
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("业务异常: {}", e.message)
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = e.message ?: "业务处理异常",
                code = 400
            )
        )
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        val message = "参数验证失败: ${errors.joinToString(", ")}"
        logger.warn("参数验证异常: {}", message)
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = message,
                code = 400
            )
        )
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(e: BindException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        val message = "参数绑定失败: ${errors.joinToString(", ")}"
        logger.warn("参数绑定异常: {}", message)
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = message,
                code = 400
            )
        )
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.constraintViolations.map { "${it.propertyPath}: ${it.message}" }
        val message = "约束验证失败: ${errors.joinToString(", ")}"
        logger.warn("约束验证异常: {}", message)
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = message,
                code = 400
            )
        )
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> {
        val message = "参数类型错误: ${e.name} 应该是 ${e.requiredType?.simpleName} 类型"
        logger.warn("参数类型异常: {}", message)
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = message,
                code = 400
            )
        )
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("非法参数异常: {}", e.message)
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = e.message ?: "参数错误",
                code = 400
            )
        )
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException::class)
    fun handleNullPointerException(e: NullPointerException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("空指针异常", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error(
                message = "系统内部错误",
                code = 500
            )
        )
    }
    
    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("未知异常", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error(
                message = "系统内部错误，请联系管理员",
                code = 500
            )
        )
    }
}