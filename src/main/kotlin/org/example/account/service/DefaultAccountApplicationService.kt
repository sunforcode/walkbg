package org.example.account.service

import org.example.account.dto.AccountProjection
import org.example.account.dto.AccountResponse
import org.example.account.dto.AuthSessionResponse
import org.example.account.dto.AvatarMediaResponse
import org.example.account.dto.LogoutResponse
import org.example.account.dto.ProfileProjection
import org.example.account.dto.ProfileResponse
import org.example.account.dto.ProfileUpdateRequest
import org.example.account.dto.VerificationCodeResponse
import org.example.account.model.AccountAvatarMedia
import org.example.account.model.AccountSession
import org.example.account.model.AccountVerification
import org.example.account.repository.AccountAvatarMediaRepository
import org.example.account.repository.AccountSessionRepository
import org.example.account.repository.AccountVerificationRepository
import org.example.common.contract.ApiContractException
import org.example.common.util.IdGenerator
import org.example.security.JwtTokenUtil
import org.example.user.model.User
import org.example.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class DefaultAccountApplicationService(
    private val userRepository: UserRepository,
    private val verificationRepository: AccountVerificationRepository,
    private val sessionRepository: AccountSessionRepository,
    private val avatarMediaRepository: AccountAvatarMediaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtil: JwtTokenUtil,
    private val verificationCodeDelivery: VerificationCodeDelivery,
    avatarMediaProperties: AvatarMediaProperties,
    private val clock: Clock = Clock.systemUTC()
) : AccountApplicationService {
    private val random = SecureRandom()
    private val avatarDirectory: Path = Paths.get(avatarMediaProperties.directory)
    private val avatarMaxSizeBytes: Long = avatarMediaProperties.maxSizeBytes

    @Transactional
    override fun sendVerificationCode(phone: String): VerificationCodeResponse {
        val now = Instant.now(clock)
        val previous = verificationRepository.findFirstByPhoneOrderByCreatedAtDesc(phone)
        if (previous != null && previous.resendAvailableAt.isAfter(now)) {
            throw ApiContractException(
                status = org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                code = "verification_send_throttled",
                message = "验证码发送过于频繁",
                retryable = true,
                details = VerificationThrottleDetails(previous.resendAvailableAt)
            )
        }
        val code = "%06d".format(random.nextInt(1_000_000))
        val record = AccountVerification(
            id = IdGenerator.generateIdWithPrefix("verification"),
            phone = phone,
            codeHash = passwordEncoder.encode(code),
            expiresAt = now.plus(Duration.ofMinutes(5)),
            resendAvailableAt = now.plus(Duration.ofSeconds(60)),
            createdAt = now
        )
        verificationRepository.save(record)
        try {
            verificationCodeDelivery.send(phone, code)
        } catch (exception: Exception) {
            throw ApiContractException.serviceUnavailable("verification_delivery_failed", "验证码发送失败，请稍后重试")
        }
        return VerificationCodeResponse(record.id, record.expiresAt, record.resendAvailableAt)
    }

    @Transactional
    override fun createSession(verificationId: String, code: String): AuthSessionResponse {
        val now = Instant.now(clock)
        val verification = verificationRepository.findByIdForUpdate(verificationId)
            ?: throw ApiContractException.unprocessable("verification_code_invalid", "验证身份或验证码无效")
        if (verification.consumedAt != null || !verification.expiresAt.isAfter(now)) {
            throw ApiContractException.unprocessable("verification_code_expired", "验证码已过期或已使用")
        }
        if (!passwordEncoder.matches(code, verification.codeHash)) {
            throw ApiContractException.unprocessable("verification_code_invalid", "验证码无效")
        }
        verification.consumedAt = now
        verificationRepository.save(verification)

        val account = userRepository.findByPhone(verification.phone) ?: userRepository.save(newAccount(verification.phone, now))
        val session = sessionRepository.save(
            AccountSession(
                id = IdGenerator.generateIdWithPrefix("session"),
                accountId = account.id,
                createdAt = now
            )
        )
        val token = jwtTokenUtil.generateAccountSessionToken(account.id, session.id)
        return AuthSessionResponse(token)
    }

    @Transactional(readOnly = true)
    override fun getAccount(accountId: String): AccountResponse {
        val account = ownedAccount(accountId)
        return AccountResponse(
            account = AccountProjection(account.id),
            profile = account.toProfile()
        )
    }

    @Transactional
    override fun createAvatarMedia(accountId: String, contentType: String, content: ByteArray): AvatarMediaResponse {
        ownedAccount(accountId)
        if (content.isEmpty()) {
            throw ApiContractException.unprocessable("avatar_media_invalid", "头像媒体内容不能为空")
        }
        if (content.size.toLong() > avatarMaxSizeBytes) {
            throw ApiContractException.unprocessable("avatar_media_too_large", "头像媒体超过大小限制")
        }
        val normalizedContentType = contentType.substringBefore(';').lowercase()
        val extension = when (normalizedContentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw ApiContractException(
                org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "avatar_media_type_unsupported",
                "不支持该头像媒体类型"
            )
        }
        Files.createDirectories(avatarDirectory)
        val mediaId = IdGenerator.generateIdWithPrefix("avatar_media")
        val mediaReference = IdGenerator.generateIdWithPrefix("media")
        val storageName = "$mediaId.$extension"
        val storagePath = avatarDirectory.resolve(storageName)
        Files.write(storagePath, content, StandardOpenOption.CREATE_NEW)
        try {
            avatarMediaRepository.save(
                AccountAvatarMedia(
                    id = mediaId,
                    mediaReference = mediaReference,
                    accountId = accountId,
                    contentType = normalizedContentType,
                    byteSize = content.size.toLong(),
                    storageName = storageName,
                    createdAt = Instant.now(clock)
                )
            )
        } catch (exception: Exception) {
            Files.deleteIfExists(storagePath)
            throw exception
        }
        return AvatarMediaResponse(mediaReference)
    }

    @Transactional
    override fun updateProfile(accountId: String, request: ProfileUpdateRequest): ProfileResponse {
        val account = ownedAccount(accountId)
        if (
            request.hasAvatar &&
            request.avatar != null &&
            !avatarMediaRepository.existsByMediaReferenceAndAccountId(request.avatar!!, accountId)
        ) {
            throw ApiContractException.unprocessable("profile_update_invalid", "头像媒体引用无效")
        }
        if (request.hasNickname) account.nickname = request.nickname
        if (request.hasAvatar) account.avatarUrl = request.avatar
        account.updatedAt = Instant.now(clock)
        return ProfileResponse(userRepository.save(account).toProfile())
    }

    @Transactional
    override fun logout(accountId: String, sessionId: String): LogoutResponse {
        val session = sessionRepository.findById(sessionId).orElseThrow {
            ApiContractException.authenticationRequired()
        }
        if (session.accountId != accountId || session.revokedAt != null) {
            throw ApiContractException.authenticationRequired()
        }
        session.revokedAt = Instant.now(clock)
        sessionRepository.save(session)
        return LogoutResponse(true)
    }

    private fun ownedAccount(accountId: String): User =
        userRepository.findById(accountId).orElseThrow { ApiContractException.notFound() }

    private fun newAccount(phone: String, now: Instant): User {
        val identity = IdGenerator.generateIdWithPrefix("account")
        return User(
            id = identity,
            username = identity,
            nickname = null,
            email = "$identity@account.invalid",
            phone = phone,
            password = passwordEncoder.encode(IdGenerator.generateIdWithPrefix("credential")),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun User.toProfile() = ProfileProjection(
        identity = id,
        maskedPhone = maskPhone(phone ?: ""),
        nickname = nickname,
        avatar = avatarUrl
    )

    private fun maskPhone(phone: String): String {
        if (!phone.matches(Regex("^\\+[1-9][0-9]{1,14}$")) || phone.length <= 5) return phone
        val digits = phone.drop(1)
        if (digits.startsWith("86") && digits.length == 13) {
            return "+" + digits.take(5) + "*".repeat(4) + digits.takeLast(4)
        }
        val callingCodeLength = when {
            digits.first() == '1' || digits.first() == '7' -> 1
            digits.take(2) in TWO_DIGIT_CALLING_CODES -> 2
            else -> 3
        }
        val visiblePrefixLength = 1 + callingCodeLength
        return phone.take(visiblePrefixLength) +
            "*".repeat((phone.length - visiblePrefixLength - 4).coerceAtLeast(0)) +
            phone.takeLast(4)
    }

    private companion object {
        val TWO_DIGIT_CALLING_CODES = setOf(
            "20", "27", "30", "31", "32", "33", "34", "36", "39", "40", "41", "43", "44", "45",
            "46", "47", "48", "49", "51", "52", "53", "54", "55", "56", "57", "58", "60", "61",
            "62", "63", "64", "65", "66", "81", "82", "84", "86", "90", "91", "92", "93", "94",
            "95", "98"
        )
    }
}

data class VerificationThrottleDetails(val retryAvailableAt: Instant)
