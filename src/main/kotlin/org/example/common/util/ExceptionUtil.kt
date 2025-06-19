package org.example.common.util

import org.example.common.exception.BusinessException
import org.slf4j.LoggerFactory

/**
 * 异常处理工具类
 * 提供常用的异常处理方法和业务异常创建方法
 */
object ExceptionUtil {

    // 将logger改为internal，允许inline函数访问
    @JvmStatic
    internal val logger = LoggerFactory.getLogger(ExceptionUtil::class.java)

    /**
     * 断言条件为真，否则抛出业务异常
     */
    fun assertTrue(condition: Boolean, message: String, errorCode: String = "ASSERTION_FAILED") {
        if (!condition) {
            throw BusinessException.badRequest(message, errorCode)
        }
    }

    /**
     * 断言条件为假，否则抛出业务异常
     */
    fun assertFalse(condition: Boolean, message: String, errorCode: String = "ASSERTION_FAILED") {
        if (condition) {
            throw BusinessException.badRequest(message, errorCode)
        }
    }

    /**
     * 断言对象不为空，否则抛出业务异常
     */
    fun <T> assertNotNull(obj: T?, message: String, errorCode: String = "NULL_VALUE"): T {
        return obj ?: throw BusinessException.badRequest(message, errorCode)
    }

    /**
     * 断言字符串不为空，否则抛出业务异常
     */
    fun assertNotBlank(str: String?, message: String, errorCode: String = "BLANK_VALUE"): String {
        if (str.isNullOrBlank()) {
            throw BusinessException.badRequest(message, errorCode)
        }
        return str
    }

    /**
     * 断言集合不为空，否则抛出业务异常
     */
    fun <T> assertNotEmpty(collection: Collection<T>?, message: String, errorCode: String = "EMPTY_COLLECTION"): Collection<T> {
        if (collection.isNullOrEmpty()) {
            throw BusinessException.badRequest(message, errorCode)
        }
        return collection
    }

    /**
     * 检查资源是否存在，不存在则抛出404异常
     */
    fun <T> checkResourceExists(resource: T?, resourceName: String, id: String): T {
        return resource ?: throw BusinessException.resourceNotFound(resourceName, id)
    }

    /**
     * 检查是否有权限访问资源
     */
    fun checkPermission(hasPermission: Boolean, message: String = "权限不足") {
        if (!hasPermission) {
            throw BusinessException.forbidden(message)
        }
    }

    /**
     * 检查资源是否重复
     */
    fun checkDuplicate(exists: Boolean, resourceName: String, field: String, value: String) {
        if (exists) {
            throw BusinessException.duplicate(resourceName, field, value)
        }
    }

    /**
     * 安全执行代码块，捕获异常并转换为业务异常
     * 注意：不使用inline以避免访问internal成员的编译错误
     */
    fun <T> safeExecute(
        operation: () -> T,
        errorMessage: String = "操作执行失败",
        errorCode: String = "OPERATION_FAILED"
    ): T {
        return try {
            operation()
        } catch (ex: BusinessException) {
            // 业务异常直接抛出
            throw ex
        } catch (ex: Exception) {
            logger.error("操作执行异常: $errorMessage", ex)
            throw BusinessException.internalError(errorMessage, errorCode, ex)
        }
    }

    /**
     * 验证参数格式
     */
    fun validateFormat(value: String, pattern: Regex, message: String, errorCode: String = "INVALID_FORMAT") {
        if (!pattern.matches(value)) {
            throw BusinessException.validation(message, errorCode)
        }
    }

    /**
     * 验证邮箱格式
     */
    fun validateEmail(email: String) {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        validateFormat(email, emailPattern, "邮箱格式不正确", "INVALID_EMAIL")
    }

    /**
     * 验证手机号格式
     */
    fun validatePhone(phone: String) {
        val phonePattern = Regex("^1[3-9]\\d{9}$")
        validateFormat(phone, phonePattern, "手机号格式不正确", "INVALID_PHONE")
    }

    /**
     * 验证密码强度
     */
    fun validatePassword(password: String) {
        assertNotBlank(password, "密码不能为空", "EMPTY_PASSWORD")
        assertTrue(password.length >= 6, "密码长度不能少于6位", "PASSWORD_TOO_SHORT")
        assertTrue(password.length <= 20, "密码长度不能超过20位", "PASSWORD_TOO_LONG")
    }

    /**
     * 验证分页参数
     */
    fun validatePageParams(page: Int, size: Int) {
        assertTrue(page >= 0, "页码不能小于0", "INVALID_PAGE")
        assertTrue(size > 0, "每页大小必须大于0", "INVALID_SIZE")
        assertTrue(size <= 100, "每页大小不能超过100", "SIZE_TOO_LARGE")
    }
}
