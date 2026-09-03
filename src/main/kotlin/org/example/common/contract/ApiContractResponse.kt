package org.example.common.contract

import com.fasterxml.jackson.annotation.JsonInclude

data class DataResponse<T>(
    val data: T
)

data class ErrorResponse(
    val error: ApiError
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val retryable: Boolean,
    val details: Any? = null
)
