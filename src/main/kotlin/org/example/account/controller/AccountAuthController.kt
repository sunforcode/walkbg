package org.example.account.controller

import org.example.account.dto.AuthSessionResponse
import org.example.account.dto.CreateAuthSessionRequest
import org.example.account.dto.LogoutResponse
import org.example.account.dto.SendVerificationCodeRequest
import org.example.account.dto.VerificationCodeResponse
import org.example.account.service.AccountApplicationService
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.example.security.CustomUserDetails
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AccountAuthController(
    private val accountApplicationService: AccountApplicationService
) {
    private val phonePattern = Regex("^\\+[1-9][0-9]{1,14}$")

    @PostMapping("/verification-codes")
    fun sendVerificationCode(
        @RequestBody request: SendVerificationCodeRequest
    ): ResponseEntity<DataResponse<VerificationCodeResponse>> {
        rejectUnknownFields(request)
        if (!phonePattern.matches(request.phone)) {
            throw ApiContractException.unprocessable("phone_invalid", "手机号必须使用有效的国际格式")
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DataResponse(accountApplicationService.sendVerificationCode(request.phone)))
    }

    @PostMapping("/session")
    fun createSession(
        @RequestBody request: CreateAuthSessionRequest
    ): ResponseEntity<DataResponse<AuthSessionResponse>> {
        rejectUnknownFields(request)
        if (request.verificationId.isBlank() || request.code.isBlank()) {
            throw ApiContractException.unprocessable("verification_input_invalid", "验证身份和验证码不能为空")
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DataResponse(accountApplicationService.createSession(request.verificationId, request.code)))
    }

    @DeleteMapping("/session")
    fun logout(authentication: Authentication?): DataResponse<LogoutResponse> {
        val principal = authentication.accountPrincipal()
        val sessionId = principal.sessionId
            ?: throw ApiContractException.authenticationRequired()
        return DataResponse(accountApplicationService.logout(principal.userId, sessionId))
    }
}

internal fun Authentication?.accountPrincipal(): CustomUserDetails {
    val principal = this?.principal
    return principal as? CustomUserDetails
        ?: throw ApiContractException.authenticationRequired()
}

private fun rejectUnknownFields(request: org.example.account.dto.StrictRequest) {
    if (request.hasUnknownFields()) {
        throw ApiContractException.invalidRequest("请求包含未定义字段")
    }
}
