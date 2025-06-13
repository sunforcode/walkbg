package org.example.common.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * 统一的API响应格式
 */
data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String = "操作成功",
    val data: T? = null,
    @JsonProperty("timestamp")
    val timestamp: Instant = Instant.now(),
    val code: Int = 200
) {
    companion object {
        /**
         * 成功响应
         */
        fun <T> success(data: T? = null, message: String = "操作成功"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = data,
                code = 200
            )
        }

        /**
         * 失败响应
         */
        fun <T> error(message: String, code: Int = 400, data: T? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                message = message,
                data = data,
                code = code
            )
        }

        /**
         * 分页成功响应
         */
        fun <T> successPage(data: T, message: String = "查询成功"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = data,
                code = 200
            )
        }
    }
}