package org.example.account.service

import org.example.account.dto.AccountResponse
import org.example.account.dto.AuthSessionResponse
import org.example.account.dto.AvatarMediaResponse
import org.example.account.dto.LogoutResponse
import org.example.account.dto.ProfileResponse
import org.example.account.dto.ProfileUpdateRequest
import org.example.account.dto.VerificationCodeResponse

interface AccountApplicationService {
    fun sendVerificationCode(phone: String): VerificationCodeResponse
    fun createSession(verificationId: String, code: String): AuthSessionResponse
    fun getAccount(accountId: String): AccountResponse
    fun createAvatarMedia(accountId: String, contentType: String, content: ByteArray): AvatarMediaResponse
    fun updateProfile(accountId: String, request: ProfileUpdateRequest): ProfileResponse
    fun logout(accountId: String, sessionId: String): LogoutResponse
}
