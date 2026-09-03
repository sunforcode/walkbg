package org.example.account.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

fun interface VerificationCodeDelivery {
    fun send(phone: String, code: String)
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

@Component
@Profile("prod")
class UnavailableVerificationCodeDelivery : VerificationCodeDelivery {
    override fun send(phone: String, code: String) {
        throw IllegalStateException("Verification code delivery provider is not configured")
    }
}
