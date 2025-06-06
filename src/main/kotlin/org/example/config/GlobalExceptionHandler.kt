package org.example.config

import org.example.exception.*
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import jakarta.validation.ConstraintViolationException
import java.time.Instant

/**
 * 全局异常处理器
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    /**
     * 创建错误响应
     */
    private fun createErrorResponse(
        status: Int,
        message: String,
        data: Any? = null
    ): Map<String, Any?> {
        return mapOf(
            "success" to false,
            "status" to status,
            "message" to message,
            "data" to data,
            "timestamp" to Instant.now()
        )
    }

    /**
     * 资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(e: ResourceNotFoundException): ResponseEntity<Map<String, Any?>> {
        logger.warn("资源未找到: {}", e.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse(404, e.message ?: "资源不存在"))
    }
    
    /**
     * 业务逻辑异常
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<Map<String, Any?>> {
        logger.warn("业务逻辑异常: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, e.message ?: "业务处理失败"))
    }
    
    /**
     * 参数验证异常
     */
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<Map<String, Any?>> {
        logger.warn("参数验证异常: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, e.message ?: "参数验证失败"))
    }
    
    /**
     * 权限不足异常
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<Map<String, Any?>> {
        logger.warn("权限不足: {}", e.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(createErrorResponse(403, e.message ?: "权限不足"))
    }
    
    /**
     * 数据冲突异常
     */
    @ExceptionHandler(DataConflictException::class)
    fun handleDataConflictException(e: DataConflictException): ResponseEntity<Map<String, Any?>> {
        logger.warn("数据冲突: {}", e.message)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(createErrorResponse(409, e.message ?: "数据冲突"))
    }
    
    /**
     * 数据完整性违反异常
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<Map<String, Any?>> {
        logger.error("数据完整性违反: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, "数据完整性约束违反"))
    }
    
    /**
     * 方法参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any?>> {
        logger.warn("方法参数验证失败: {}", e.message)
        
        val errors = mutableMapOf<String, String>()
        e.bindingResult.fieldErrors.forEach { error ->
            errors[error.field] = error.defaultMessage ?: "验证失败"
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, "参数验证失败", errors))
    }
    
    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(e: BindException): ResponseEntity<Map<String, Any?>> {
        logger.warn("参数绑定失败: {}", e.message)
        
        val errors = mutableMapOf<String, String>()
        e.bindingResult.fieldErrors.forEach { error ->
            errors[error.field] = error.defaultMessage ?: "绑定失败"
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, "参数绑定失败", errors))
    }
    
    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<Map<String, Any?>> {
        logger.warn("约束违反: {}", e.message)
        
        val errors = e.constraintViolations.map { violation ->
            "${violation.propertyPath}: ${violation.message}"
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, "约束验证失败", errors))
    }
    
    /**
     * 方法参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<Map<String, Any?>> {
        logger.warn("方法参数类型不匹配: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, "参数类型不匹配: ${e.name}"))
    }
    
    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<Map<String, Any?>> {
        logger.warn("非法参数: {}", e.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(400, e.message ?: "参数错误"))
    }
    
    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<Map<String, Any?>> {
        logger.error("未处理的异常: ", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(500, "系统内部错误"))
    }
}