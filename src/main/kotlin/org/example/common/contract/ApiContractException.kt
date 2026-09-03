package org.example.common.contract

import org.springframework.http.HttpStatus

class ApiContractException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val retryable: Boolean = false,
    val details: Any? = null
) : RuntimeException(message) {
    companion object {
        fun invalidRequest(message: String = "请求结构无效") =
            ApiContractException(HttpStatus.BAD_REQUEST, "invalid_request", message)

        fun authenticationRequired(message: String = "需要有效的认证会话") =
            ApiContractException(HttpStatus.UNAUTHORIZED, "authentication_required", message)

        fun unprocessable(code: String, message: String) =
            ApiContractException(HttpStatus.UNPROCESSABLE_ENTITY, code, message)

        fun notFound(message: String = "资源不存在") =
            ApiContractException(HttpStatus.NOT_FOUND, "resource_not_found", message)

        fun conflict(code: String, message: String) =
            ApiContractException(HttpStatus.CONFLICT, code, message)

        fun serviceUnavailable(code: String, message: String) =
            ApiContractException(HttpStatus.SERVICE_UNAVAILABLE, code, message, retryable = true)
    }
}
