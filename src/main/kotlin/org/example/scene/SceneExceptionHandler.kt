package org.example.scene

import org.example.common.contract.ApiContractException
import org.example.common.contract.ApiError
import org.example.common.contract.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice(assignableTypes = [SceneController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class SceneExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(ApiContractException::class)
    fun contract(ex: ApiContractException) = error(ex.status, ex.code, ex.message, ex.retryable)
    @ExceptionHandler(HttpMessageNotReadableException::class, MissingServletRequestParameterException::class, MethodArgumentTypeMismatchException::class)
    fun invalid() = error(HttpStatus.BAD_REQUEST, "invalid_request", "请求结构无效")
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun unsupported() = error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "scene_media_type_unsupported", "仅支持 PNG 或 JPEG 图片")
    @ExceptionHandler(DataAccessException::class)
    fun storage(ex: DataAccessException): ResponseEntity<ErrorResponse> {
        logger.error("Scene persistence failure", ex)
        return error(HttpStatus.SERVICE_UNAVAILABLE, "scene_storage_unavailable", "场景资源暂时无法读取或保存", true)
    }
    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Scene request failure", ex)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "服务内部错误")
    }
    private fun error(status: HttpStatus, code: String, message: String, retryable: Boolean = false) =
        ResponseEntity.status(status).body(ErrorResponse(ApiError(code, message, retryable)))
}
