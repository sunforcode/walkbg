package org.example.account.controller

import org.example.account.dto.AccountResponse
import org.example.account.dto.AvatarMediaResponse
import org.example.account.dto.ProfileResponse
import org.example.account.dto.ProfileUpdateRequest
import org.example.account.service.AccountApplicationService
import org.example.common.contract.ApiContractException
import org.example.common.contract.DataResponse
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/account")
class AccountController(
    private val accountApplicationService: AccountApplicationService
) {
    @GetMapping
    fun getAccount(authentication: Authentication?): DataResponse<AccountResponse> {
        val accountId = authentication.accountPrincipal().userId
        return DataResponse(accountApplicationService.getAccount(accountId))
    }

    @PostMapping("/profile/avatar-media", consumes = [MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"])
    fun createAvatarMedia(
        authentication: Authentication?,
        @RequestHeader("Content-Type") contentType: String,
        @RequestBody(required = false) content: ByteArray?
    ): DataResponse<AvatarMediaResponse> {
        val accountId = authentication.accountPrincipal().userId
        if (content == null || content.isEmpty()) {
            throw ApiContractException.unprocessable("avatar_media_invalid", "头像媒体内容不能为空")
        }
        return DataResponse(accountApplicationService.createAvatarMedia(accountId, contentType, content))
    }

    @PatchMapping("/profile")
    fun updateProfile(
        authentication: Authentication?,
        @RequestBody request: ProfileUpdateRequest
    ): DataResponse<ProfileResponse> {
        val accountId = authentication.accountPrincipal().userId
        if (request.hasUnknownFields()) {
            throw ApiContractException.invalidRequest("请求包含未定义字段")
        }
        if (!request.hasNickname && !request.hasAvatar) {
            throw ApiContractException.unprocessable("profile_update_invalid", "至少提供一个可修改字段")
        }
        if (request.hasNickname && request.nickname?.isBlank() == true) {
            throw ApiContractException.unprocessable("profile_update_invalid", "昵称不能为空白文本")
        }
        if (request.hasAvatar && request.avatar?.isBlank() == true) {
            throw ApiContractException.unprocessable("profile_update_invalid", "头像引用不能为空白文本")
        }
        return DataResponse(accountApplicationService.updateProfile(accountId, request))
    }
}
