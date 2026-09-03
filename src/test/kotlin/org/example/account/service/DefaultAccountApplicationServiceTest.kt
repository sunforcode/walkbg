package org.example.account.service

import org.example.account.dto.AuthSessionResponse
import org.example.account.dto.ProfileUpdateRequest
import org.example.account.model.AccountAvatarMedia
import org.example.account.model.AccountVerification
import org.example.account.repository.AccountAvatarMediaRepository
import org.example.account.repository.AccountSessionRepository
import org.example.account.repository.AccountVerificationRepository
import org.example.common.contract.ApiContractException
import org.example.security.JwtTokenUtil
import org.example.user.model.User
import org.example.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.data.jpa.repository.Lock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import jakarta.persistence.LockModeType

class DefaultAccountApplicationServiceTest {
    private val now = Instant.parse("2026-09-02T10:00:00Z")
    private val userRepository = mock<UserRepository>()
    private val verificationRepository = mock<AccountVerificationRepository>()
    private val sessionRepository = mock<AccountSessionRepository>()
    private val avatarMediaRepository = mock<AccountAvatarMediaRepository>()
    private val jwtTokenUtil = mock<JwtTokenUtil>()
    private val delivery = mock<VerificationCodeDelivery>()
    private val encoder = BCryptPasswordEncoder(4)

    @TempDir
    lateinit var avatarDirectory: Path

    private fun service(maxSizeBytes: Long = 5L * 1024 * 1024) = DefaultAccountApplicationService(
        userRepository,
        verificationRepository,
        sessionRepository,
        avatarMediaRepository,
        encoder,
        jwtTokenUtil,
        delivery,
        AvatarMediaProperties().apply {
            directory = avatarDirectory.toString()
            this.maxSizeBytes = maxSizeBytes
        },
        Clock.fixed(now, ZoneOffset.UTC)
    )

    @Test
    fun `first verified phone creates account without invented nickname or avatar`() {
        val code = "001204"
        val verification = AccountVerification(
            id = "verification-1",
            phone = "+8613800138000",
            codeHash = encoder.encode(code),
            expiresAt = now.plusSeconds(300),
            resendAvailableAt = now,
            createdAt = now
        )
        whenever(verificationRepository.findByIdForUpdate("verification-1")).thenReturn(verification)
        doAnswer { it.arguments[0] }.whenever(verificationRepository).save(any<AccountVerification>())
        whenever(userRepository.findByPhone(verification.phone)).thenReturn(null)
        doAnswer { it.arguments[0] }.whenever(userRepository).save(any<User>())
        doAnswer { it.arguments[0] }.whenever(sessionRepository).save(any<org.example.account.model.AccountSession>())
        whenever(jwtTokenUtil.generateAccountSessionToken(any(), any())).thenReturn("access-token")

        service().createSession("verification-1", code)

        val account = argumentCaptor<User>()
        verify(userRepository).save(account.capture())
        assertNull(account.firstValue.nickname)
        assertNull(account.firstValue.avatarUrl)
        assertTrue(account.firstValue.username.startsWith("account_"))
    }

    @Test
    fun `verification throttle exposes retryAvailableAt details`() {
        val retryAt = now.plusSeconds(30)
        whenever(verificationRepository.findFirstByPhoneOrderByCreatedAtDesc("+8613800138000"))
            .thenReturn(verification(expiresAt = now.plusSeconds(300), resendAvailableAt = retryAt))

        val exception = assertThrows<ApiContractException> {
            service().sendVerificationCode("+8613800138000")
        }

        assertEquals("verification_send_throttled", exception.code)
        assertEquals(retryAt, (exception.details as VerificationThrottleDetails).retryAvailableAt)
    }

    @Test
    fun `verification delivery failure uses stable error code`() {
        whenever(verificationRepository.findFirstByPhoneOrderByCreatedAtDesc("+8613800138000")).thenReturn(null)
        doAnswer { it.arguments[0] }.whenever(verificationRepository).save(any<AccountVerification>())
        doThrow(IllegalStateException("provider unavailable")).whenever(delivery).send(any(), any())

        val exception = assertThrows<ApiContractException> {
            service().sendVerificationCode("+8613800138000")
        }

        assertEquals("verification_delivery_failed", exception.code)
        assertEquals(503, exception.status.value())
    }

    @Test
    fun `wrong verification code is invalid`() {
        whenever(verificationRepository.findByIdForUpdate("verification-1"))
            .thenReturn(verification(expiresAt = now.plusSeconds(300)))

        val exception = assertThrows<ApiContractException> {
            service().createSession("verification-1", "wrong")
        }

        assertEquals("verification_code_invalid", exception.code)
    }

    @Test
    fun `expired verification code is expired`() {
        whenever(verificationRepository.findByIdForUpdate("verification-1"))
            .thenReturn(verification(expiresAt = now.minusSeconds(1)))

        val exception = assertThrows<ApiContractException> {
            service().createSession("verification-1", "001204")
        }

        assertEquals("verification_code_expired", exception.code)
    }

    @Test
    fun `consumed verification code is expired`() {
        val verification = verification(expiresAt = now.plusSeconds(300)).apply { consumedAt = now.minusSeconds(1) }
        whenever(verificationRepository.findByIdForUpdate("verification-1")).thenReturn(verification)

        val exception = assertThrows<ApiContractException> {
            service().createSession("verification-1", "001204")
        }

        assertEquals("verification_code_expired", exception.code)
    }

    @Test
    fun `session creation reads verification with a pessimistic write lock`() {
        val method = AccountVerificationRepository::class.java.getMethod("findByIdForUpdate", String::class.java)

        assertEquals(LockModeType.PESSIMISTIC_WRITE, method.getAnnotation(Lock::class.java).value)
    }

    @Test
    fun `E164 phone mask preserves calling code and final four digits`() {
        val cases = listOf(
            "+8613800138000" to "+86138****8000",
            "+14155552671" to "+1******2671",
            "+447911123456" to "+44******3456",
            "+358401234567" to "+358*****4567"
        )

        cases.forEach { (phone, expected) ->
            whenever(userRepository.findById("account-1")).thenReturn(Optional.of(account(phone)))
            assertEquals(expected, service().getAccount("account-1").profile.maskedPhone)
        }
    }

    @Test
    fun `production profile starts account service with an explicitly unavailable verification delivery`() {
        ApplicationContextRunner()
            .withInitializer { it.environment.setActiveProfiles("prod") }
            .withPropertyValues(
                "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-context",
                "jwt.expiration=60000",
                "jwt.refresh-expiration=120000"
            )
            .withUserConfiguration(ProductionDeliveryTestConfiguration::class.java)
            .run { context ->
                assertNull(context.startupFailure)
                val verificationRepository = context.getBean(AccountVerificationRepository::class.java)
                whenever(verificationRepository.findFirstByPhoneOrderByCreatedAtDesc("+8613800138000"))
                    .thenReturn(null)
                doAnswer { it.arguments[0] }.whenever(verificationRepository).save(any<AccountVerification>())
                val applicationService = context.getBean(AccountApplicationService::class.java)

                val exception = assertThrows<ApiContractException> {
                    applicationService.sendVerificationCode("+8613800138000")
                }

                assertEquals("verification_delivery_failed", exception.code)
                assertEquals(503, exception.status.value())
            }
    }

    @Test
    fun `avatar media rejects content larger than the configured limit`() {
        whenever(userRepository.findById("account-1")).thenReturn(Optional.of(account()))

        val exception = assertThrows<ApiContractException> {
            service(maxSizeBytes = 4).createAvatarMedia("account-1", "image/png", ByteArray(5))
        }

        assertEquals("avatar_media_too_large", exception.code)
        verify(avatarMediaRepository, never()).save(any<AccountAvatarMedia>())
        assertFalse(Files.list(avatarDirectory).use { it.findAny().isPresent })
    }

    @Test
    fun `avatar media stores an account-owned opaque reference in the configured directory`() {
        whenever(userRepository.findById("account-1")).thenReturn(Optional.of(account()))
        doAnswer { it.arguments[0] }.whenever(avatarMediaRepository).save(any<AccountAvatarMedia>())

        val response = service().createAvatarMedia("account-1", "image/png", byteArrayOf(1, 2, 3))

        assertTrue(response.mediaReference.startsWith("media_"))
        assertFalse(response.mediaReference.contains('/'))
        val record = argumentCaptor<AccountAvatarMedia>()
        verify(avatarMediaRepository).save(record.capture())
        assertEquals("account-1", record.firstValue.accountId)
        assertEquals(response.mediaReference, record.firstValue.mediaReference)
        assertTrue(Files.exists(avatarDirectory.resolve(record.firstValue.storageName)))
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `profile update rejects an avatar reference not owned by the current account`() {
        whenever(userRepository.findById("account-1")).thenReturn(Optional.of(account()))
        whenever(avatarMediaRepository.existsByMediaReferenceAndAccountId("media_other", "account-1"))
            .thenReturn(false)
        val request = ProfileUpdateRequest().apply { assignAvatar("media_other") }

        val exception = assertThrows<ApiContractException> {
            service().updateProfile("account-1", request)
        }

        assertEquals("profile_update_invalid", exception.code)
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `profile update accepts an avatar reference owned by the current account`() {
        val account = account()
        whenever(userRepository.findById("account-1")).thenReturn(Optional.of(account))
        whenever(avatarMediaRepository.existsByMediaReferenceAndAccountId("media_owned", "account-1"))
            .thenReturn(true)
        doAnswer { it.arguments[0] }.whenever(userRepository).save(any<User>())
        val request = ProfileUpdateRequest().apply { assignAvatar("media_owned") }

        val response = service().updateProfile("account-1", request)

        assertEquals("media_owned", response.profile.avatar)
        assertEquals("media_owned", account.avatarUrl)
    }

    private fun verification(
        expiresAt: Instant,
        resendAvailableAt: Instant = now
    ) = AccountVerification(
        id = "verification-1",
        phone = "+8613800138000",
        codeHash = encoder.encode("001204"),
        expiresAt = expiresAt,
        resendAvailableAt = resendAvailableAt,
        createdAt = now
    )

    private fun account(phone: String = "+8613800138000") = User(
        id = "account-1",
        username = "account-1",
        nickname = "Walker",
        email = "account-1@account.invalid",
        phone = phone,
        password = "encoded",
        createdAt = now,
        updatedAt = now
    )
}

@TestConfiguration(proxyBeanMethods = false)
@Import(LoggingVerificationCodeDelivery::class, UnavailableVerificationCodeDelivery::class, DefaultAccountApplicationService::class)
private class ProductionDeliveryTestConfiguration {
    @Bean
    fun userRepository(): UserRepository = mock()

    @Bean
    fun verificationRepository(): AccountVerificationRepository = mock()

    @Bean
    fun sessionRepository(): AccountSessionRepository = mock()

    @Bean
    fun avatarMediaRepository(): AccountAvatarMediaRepository = mock()

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder(4)

    @Bean
    fun jwtTokenUtil(): JwtTokenUtil = mock()

    @Bean
    fun avatarMediaProperties(): AvatarMediaProperties = AvatarMediaProperties()

    @Bean
    fun clock(): Clock = Clock.fixed(Instant.parse("2026-09-02T10:00:00Z"), ZoneOffset.UTC)
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:prod-startup;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "kml-agent.base-url=http://localhost:8001",
        "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-context",
        "jwt.expiration=60000",
        "jwt.refresh-expiration=120000",
        "cors.allowed-origins=*"
    ]
)
@ActiveProfiles("prod")
class ProductionAccountApplicationContextTest {
    @MockBean
    private lateinit var appInitializer: org.example.config.AppInitializer

    @Autowired
    private lateinit var verificationCodeDelivery: VerificationCodeDelivery

    @Test
    fun `production application context supplies unavailable verification delivery`() {
        assertTrue(verificationCodeDelivery is UnavailableVerificationCodeDelivery)
    }
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:account-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "kml-agent.base-url=http://127.0.0.1:65535",
        "kml-agent.enabled=false",
        "jwt.secret=account-concurrency-test-secret-key-at-least-256-bits-long",
        "jwt.expiration=60000",
        "jwt.refresh-expiration=120000",
        "cors.allowed-origins=*",
        "account.avatar-media.directory=/tmp/walkbg-account-concurrency-avatar"
    ]
)
@ActiveProfiles("test")
class AccountVerificationConcurrencyIntegrationTest {
    @MockBean
    private lateinit var appInitializer: org.example.config.AppInitializer

    @Autowired
    private lateinit var service: AccountApplicationService

    @Autowired
    private lateinit var verificationRepository: AccountVerificationRepository

    @Autowired
    private lateinit var sessionRepository: AccountSessionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @org.junit.jupiter.api.BeforeEach
    fun seedVerification() {
        sessionRepository.deleteAll()
        verificationRepository.deleteAll()
        userRepository.deleteAll()
        val now = Instant.now()
        verificationRepository.saveAndFlush(
            AccountVerification(
                id = "concurrent-verification",
                phone = "+358401234567",
                codeHash = passwordEncoder.encode("001204"),
                expiresAt = now.plusSeconds(300),
                resendAvailableAt = now,
                createdAt = now
            )
        )
    }

    @Test
    fun `concurrent consumption of one verification creates only one session`() {
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val attempts = (1..2).map {
                executor.submit<Result<AuthSessionResponse>> {
                    ready.countDown()
                    start.await(5, TimeUnit.SECONDS)
                    runCatching { service.createSession("concurrent-verification", "001204") }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val results = attempts.map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it.isSuccess })
            val failure = results.single { it.isFailure }.exceptionOrNull()
            assertTrue(failure is ApiContractException)
            assertEquals("verification_code_expired", (failure as ApiContractException).code)
            assertEquals(1L, sessionRepository.count())
            assertTrue(requireNotNull(verificationRepository.findById("concurrent-verification").orElse(null)).consumedAt != null)
        } finally {
            executor.shutdownNow()
        }
    }
}
