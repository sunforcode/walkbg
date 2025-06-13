package org.example.common.util

import org.example.common.dto.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * 响应工具类
 */
object ResponseUtil {
    
    /**
     * 成功响应
     */
    fun <T> success(data: T? = null, message: String = "操作成功"): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.ok(ApiResponse.success(data, message))
    }
    
    /**
     * 创建成功响应
     */
    fun <T> created(data: T, message: String = "创建成功"): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data, message))
    }
    
    /**
     * 分页成功响应
     */
    fun <T> successPage(data: Page<T>, message: String = "查询成功"): ResponseEntity<ApiResponse<Page<T>>> {
        return ResponseEntity.ok(ApiResponse.successPage(data, message))
    }
    
    /**
     * 无内容响应
     */
    fun noContent(message: String = "操作成功"): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.ok(ApiResponse.success(null, message))
    }
    
    /**
     * 错误响应
     */
    fun <T> error(message: String, code: Int = 400): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.status(code)
            .body(ApiResponse.error(message, code))
    }
    
    /**
     * 未找到响应
     */
    fun <T> notFound(message: String = "资源未找到"): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(message, 404))
    }
    
    /**
     * 条件响应 - 根据数据是否存在返回成功或未找到
     */
    fun <T> conditional(data: T?, successMessage: String = "操作成功", notFoundMessage: String = "资源未找到"): ResponseEntity<ApiResponse<T>> {
        return if (data != null) {
            success(data, successMessage)
        } else {
            notFound(notFoundMessage)
        }
    }
    
    /**
     * 条件响应 - 根据操作结果返回成功或失败
     */
    fun conditionalOperation(
        success: Boolean, 
        successMessage: String = "操作成功", 
        failMessage: String = "操作失败"
    ): ResponseEntity<ApiResponse<Nothing>> {
        return if (success) {
            success(null, successMessage)
        } else {
            error(failMessage, 400)
        }
    }
}