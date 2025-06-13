package org.example.common.exception

/**
 * 资源未找到异常
 */
class ResourceNotFoundException(message: String) : RuntimeException(message)

/**
 * 业务逻辑异常
 */
class BusinessException(message: String) : RuntimeException(message) {
    companion object {
        fun badRequest(message: String): BusinessException = BusinessException(message)
        fun conflict(message: String): BusinessException = BusinessException(message)
    }
}

/**
 * 参数验证异常
 */
class ValidationException(message: String) : RuntimeException(message)

/**
 * 权限不足异常
 */
class AccessDeniedException(message: String) : RuntimeException(message)

/**
 * 数据冲突异常
 */
class DataConflictException(message: String) : RuntimeException(message)