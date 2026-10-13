package org.example.account.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

fun interface VerificationCodeDelivery {
    fun send(phone: String, code: String)
}

@Component
@ConfigurationProperties(prefix = "account.verification-code")
class VerificationCodeProperties {
    /**
     * true 时跳过短信下发与验证码比对（任意验证码可登录）。
     * 仅用于短信服务商接入前的过渡期，默认关闭。
     */
    var bypass: Boolean = false
}

@Component
@ConfigurationProperties(prefix = "account.avatar-media")
class AvatarMediaProperties {
    var directory: String = "data/avatar-media"
    var maxSizeBytes: Long = 5L * 1024 * 1024
}

@Component
@Profile("!prod")
class LoggingVerificationCodeDelivery : VerificationCodeDelivery {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(phone: String, code: String) {
        logger.info("Development verification code issued for {}: {}", phone.takeLast(4).padStart(phone.length, '*'), code)
    }
}
