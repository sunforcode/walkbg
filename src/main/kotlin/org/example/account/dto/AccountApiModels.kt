package org.example.account.dto

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import java.time.Instant

abstract class StrictRequest {
    private val unknownFields = linkedSetOf<String>()

    @JsonAnySetter
    fun captureUnknownField(name: String, value: Any?) {
        unknownFields += name
    }

    fun hasUnknownFields(): Boolean = unknownFields.isNotEmpty()
}

class SendVerificationCodeRequest(
    val phone: String = ""
) : StrictRequest()

class CreateAuthSessionRequest(
    val verificationId: String = "",
    val code: String = ""
) : StrictRequest()

data class VerificationCodeResponse(
    val verificationId: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val expiresAt: Instant,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val resendAvailableAt: Instant
)

data class AuthSessionResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)

data class AccountProjection(
    val identity: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileProjection(
    val identity: String,
    val maskedPhone: String,
    val nickname: String? = null,
    val avatar: String? = null
)

data class AccountResponse(
    val account: AccountProjection,
    val profile: ProfileProjection
)

data class ProfileResponse(
    val profile: ProfileProjection
)

data class AvatarMediaResponse(
    val mediaReference: String
)

data class LogoutResponse(
    val loggedOut: Boolean
)

class ProfileUpdateRequest : StrictRequest() {
    var nickname: String? = null
        private set
    var avatar: String? = null
        private set
    var hasNickname: Boolean = false
        private set
    var hasAvatar: Boolean = false
        private set

    @JsonSetter("nickname")
    fun assignNickname(value: String?) {
        hasNickname = true
        nickname = value
    }

    @JsonSetter("avatar")
    fun assignAvatar(value: String?) {
        hasAvatar = true
        avatar = value
    }
}
