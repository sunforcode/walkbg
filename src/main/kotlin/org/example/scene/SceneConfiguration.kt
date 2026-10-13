package org.example.scene

import org.example.common.contract.ApiContractException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.security.MessageDigest

@Component
class SceneConfiguration(
    @Value("\${app.scenes.public-base-url:}") private val publicBaseUrl: String,
    @Value("\${app.scenes.management-token:}") private val managementToken: String
) {
    fun authorize(token: String?) {
        if (managementToken.isBlank()) throw ApiContractException.serviceUnavailable("scene_management_unavailable", "场景管理尚未配置")
        if (!MessageDigest.isEqual(managementToken.toByteArray(Charsets.UTF_8), (token ?: "").toByteArray(Charsets.UTF_8))) {
            throw ApiContractException(org.springframework.http.HttpStatus.UNAUTHORIZED, "scene_management_authentication_required", "需要有效的场景管理凭据")
        }
    }

    fun imageUrl(mediaId: String): String {
        val base = try { URI(publicBaseUrl) } catch (_: Exception) { null }
        if (base == null || base.scheme !in setOf("http", "https") || base.host.isNullOrBlank() ||
            base.userInfo != null || base.query != null || base.fragment != null || base.normalize() != base ||
            base.path.split('/').any { it == "." || it == ".." }) {
            throw ApiContractException.serviceUnavailable("scene_media_unavailable", "场景图片服务尚未配置")
        }
        return publicBaseUrl.trimEnd('/') + "/api/v1/scene-media/" + mediaId
    }
}
