package org.example.account.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 腾讯云短信验证码下发（生产环境）。
 *
 * 设计约束：
 * - 不引入腾讯云 SDK，直接按 TC3-HMAC-SHA256 签名规范调用 SendSms API；
 * - 凭证缺失时保持与历史 [UnavailableVerificationCodeDelivery] 相同的行为：
 *   send 时抛 IllegalStateException，由全局异常处理转成 verification_delivery_failed；
 *   不在启动期失败，避免未配置凭证时阻塞部署。
 */
@Component
@Profile("prod")
class TencentCloudSmsVerificationCodeDelivery(
    private val properties: TencentSmsProperties,
    private val clock: Clock = Clock.systemUTC(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val httpPost: (host: String, headers: Map<String, String>, body: String) -> String =
        defaultHttpPost(properties),
) : VerificationCodeDelivery {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(phone: String, code: String) {
        if (!properties.isConfigured()) {
            throw IllegalStateException("Verification code delivery provider is not configured")
        }
        val timestamp = clock.instant().epochSecond
        val payload = buildPayload(phone, code)
        val headers = buildSignedHeaders(payload, timestamp)
        val responseBody = try {
            httpPost(SMS_HOST, headers, payload)
        } catch (exception: Exception) {
            throw IllegalStateException("Verification code delivery request failed", exception)
        }
        assertSendAccepted(phone, responseBody)
    }

    internal fun buildPayload(phone: String, code: String): String {
        require(phone.startsWith("+")) { "phone must be in E.164 format" }
        val payload = mapOf(
            "PhoneNumberSet" to listOf(phone),
            "SmsSdkAppId" to properties.sdkAppId,
            "SignName" to properties.signName,
            "TemplateId" to properties.templateId,
            "TemplateParamSet" to listOf(code),
        )
        return objectMapper.writeValueAsString(payload)
    }

    internal fun buildSignedHeaders(payload: String, timestamp: Long): Map<String, String> {
        val canonicalRequest = listOf(
            "POST",
            "/",
            "",
            "content-type:${CONTENT_TYPE}\nhost:$SMS_HOST\nx-tc-action:${ACTION.lowercase()}\n",
            "content-type;host;x-tc-action",
            sha256Hex(payload),
        ).joinToString("\n")

        val instant = Instant.ofEpochSecond(timestamp)
        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(instant)
        val stringToSign = listOf(
            ALGORITHM,
            timestamp.toString(),
            "$date/${SERVICE}/tc3_request",
            sha256Hex(canonicalRequest),
        ).joinToString("\n")

        val signature = hmacSha256Hex(
            key = hmacChain(properties.secretKey, date),
            data = stringToSign,
        )

        return mapOf(
            "Content-Type" to CONTENT_TYPE,
            "Host" to SMS_HOST,
            "X-TC-Action" to ACTION,
            "X-TC-Version" to VERSION,
            "X-TC-Region" to properties.region,
            "X-TC-Timestamp" to timestamp.toString(),
            "Authorization" to (
                "$ALGORITHM Credential=${properties.secretId}/$date/$SERVICE/tc3_request, " +
                    "SignedHeaders=content-type;host;x-tc-action, Signature=$signature"
                ),
        )
    }

    private fun hmacChain(secretKey: String, date: String): ByteArray {
        val secretDate = hmacSha256("TC3$secretKey".toByteArray(), date)
        val secretService = hmacSha256(secretDate, SERVICE)
        return hmacSha256(secretService, "tc3_request")
    }

    private fun assertSendAccepted(phone: String, responseBody: String) {
        val root = objectMapper.readTree(responseBody)
        root.path("Response").path("Error").takeIf { !it.isMissingNode }?.let { error ->
            throw IllegalStateException(
                "Verification code delivery rejected: ${error.path("Code").asText()} ${error.path("Message").asText()}",
            )
        }
        val sendStatus = root.path("Response").path("SendStatusSet").firstOrNull()
            ?: throw IllegalStateException("Verification code delivery response missing SendStatusSet")
        val statusCode = sendStatus.path("Code").asText()
        if (statusCode != "Ok") {
            throw IllegalStateException(
                "Verification code delivery failed for ${phone.takeLast(4).padStart(phone.length, '*')}: " +
                    "$statusCode ${sendStatus.path("Message").asText()}",
            )
        }
        logger.info("Verification code sent via Tencent SMS to {}", phone.takeLast(4).padStart(phone.length, '*'))
    }

    private companion object {
        const val SMS_HOST = "sms.tencentcloudapi.com"
        const val ACTION = "SendSms"
        const val VERSION = "2021-01-11"
        const val SERVICE = "sms"
        const val ALGORITHM = "TC3-HMAC-SHA256"
        const val CONTENT_TYPE = "application/json; charset=utf-8"

        fun sha256Hex(data: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(data.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

        fun hmacSha256(key: ByteArray, data: String): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(data.toByteArray(Charsets.UTF_8))
        }

        fun hmacSha256Hex(key: ByteArray, data: String): String =
            hmacSha256(key, data).joinToString("") { "%02x".format(it) }

        fun defaultHttpPost(properties: TencentSmsProperties): (String, Map<String, String>, String) -> String {
            val client = WebClient.builder().baseUrl("https://$SMS_HOST").build()
            return { _, headers, body ->
                client.post()
                    .uri("/")
                    .headers { httpHeaders -> headers.forEach { (name, value) -> httpHeaders.set(name, value) } }
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block(java.time.Duration.ofSeconds(properties.timeoutSeconds))
                    ?: throw IllegalStateException("Verification code delivery response is empty")
            }
        }
    }
}

@Component
@ConfigurationProperties(prefix = "account.verification-delivery.tencent")
class TencentSmsProperties {
    var secretId: String = ""
    var secretKey: String = ""
    var sdkAppId: String = ""
    var signName: String = ""
    var templateId: String = ""
    var region: String = "ap-guangzhou"
    var timeoutSeconds: Long = 10

    fun isConfigured(): Boolean =
        secretId.isNotBlank() && secretKey.isNotBlank() && sdkAppId.isNotBlank() &&
            signName.isNotBlank() && templateId.isNotBlank()
}