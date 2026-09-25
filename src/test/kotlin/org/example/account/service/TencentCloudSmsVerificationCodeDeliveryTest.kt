package org.example.account.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TencentCloudSmsVerificationCodeDeliveryTest {

    private val fixedClock = Clock.fixed(Instant.ofEpochSecond(1789280943), ZoneOffset.UTC)

    private fun configuredProperties() = TencentSmsProperties().apply {
        secretId = "AKIDtest123"
        secretKey = "secretKeyTest456"
        sdkAppId = "140000000"
        signName = "Walk公众号"
        templateId = "1000001"
    }

    @Test
    fun `send throws when credentials are missing`() {
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = TencentSmsProperties(),
            clock = fixedClock,
            httpPost = { _, _, _ -> throw AssertionError("must not call http") },
        )

        assertThatThrownBy { delivery.send("+8613800138000", "123456") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("not configured")
    }

    @Test
    fun `payload contains phone code and template`() {
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = configuredProperties(),
            clock = fixedClock,
            httpPost = { _, _, _ -> "{}" },
        )

        val payload = delivery.buildPayload("+8613800138000", "123456")

        assertThat(payload).contains("\"+8613800138000\"")
        assertThat(payload).contains("\"123456\"")
        assertThat(payload).contains("\"140000000\"")
        assertThat(payload).contains("\"1000001\"")
        assertThat(payload).contains("\"Walk公众号\"")
    }

    @Test
    fun `signed headers follow TC3 shape`() {
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = configuredProperties(),
            clock = fixedClock,
            httpPost = { _, _, _ -> "{}" },
        )

        val headers = delivery.buildSignedHeaders("{}", fixedClock.instant().epochSecond)

        assertThat(headers["X-TC-Action"]).isEqualTo("SendSms")
        assertThat(headers["X-TC-Version"]).isEqualTo("2021-01-11")
        assertThat(headers["X-TC-Timestamp"]).isEqualTo("1789280943")
        assertThat(headers["Content-Type"]).isEqualTo("application/json; charset=utf-8")
        val authorization = headers["Authorization"] ?: ""
        assertThat(authorization).startsWith("TC3-HMAC-SHA256 Credential=AKIDtest123/")
        assertThat(authorization).contains("/2026-09-13/sms/tc3_request")
        assertThat(authorization).contains("SignedHeaders=content-type;host;x-tc-action")
        val signature = authorization.substringAfter("Signature=")
        assertThat(signature).hasSize(64).matches("[0-9a-f]+")
    }

    @Test
    fun `send succeeds when provider returns Ok`() {
        val captured = mutableMapOf<String, String>()
        val responseBody = """
            {"Response":{"SendStatusSet":[{"SerialNo":"sn1","PhoneNumber":"+8613800138000","Fee":1,"Code":"Ok","Message":"send success"}],"RequestId":"req-1"}}
        """.trimIndent()
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = configuredProperties(),
            clock = fixedClock,
            httpPost = { host, headers, body ->
                captured["host"] = host
                captured["body"] = body
                captured.putAll(headers)
                responseBody
            },
        )

        delivery.send("+8613800138000", "123456")

        assertThat(captured["host"]).isEqualTo("sms.tencentcloudapi.com")
        assertThat(captured["body"]).contains("123456")
    }

    @Test
    fun `send throws when SendStatus code is not Ok`() {
        val responseBody = """
            {"Response":{"SendStatusSet":[{"SerialNo":"sn1","PhoneNumber":"+8613800138000","Fee":0,"Code":"LimitExceeded.PhoneNumberDailyLimit","Message":"delivery limit"}],"RequestId":"req-1"}}
        """.trimIndent()
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = configuredProperties(),
            clock = fixedClock,
            httpPost = { _, _, _ -> responseBody },
        )

        assertThatThrownBy { delivery.send("+8613800138000", "123456") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("LimitExceeded.PhoneNumberDailyLimit")
    }

    @Test
    fun `send throws when provider returns api error`() {
        val responseBody = """
            {"Response":{"Error":{"Code":"AuthFailure.SignatureFailure","Message":"signature invalid"},"RequestId":"req-1"}}
        """.trimIndent()
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = configuredProperties(),
            clock = fixedClock,
            httpPost = { _, _, _ -> responseBody },
        )

        assertThatThrownBy { delivery.send("+8613800138000", "123456") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("AuthFailure.SignatureFailure")
    }

    @Test
    fun `send throws when transport fails`() {
        val delivery = TencentCloudSmsVerificationCodeDelivery(
            properties = configuredProperties(),
            clock = fixedClock,
            httpPost = { _, _, _ -> throw java.io.IOException("connection reset") },
        )

        assertThatThrownBy { delivery.send("+8613800138000", "123456") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("request failed")
    }
}
