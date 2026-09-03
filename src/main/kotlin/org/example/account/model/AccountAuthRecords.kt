package org.example.account.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "account_verifications",
    indexes = [Index(name = "idx_account_verification_phone", columnList = "phone")]
)
data class AccountVerification(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 32)
    val phone: String,

    @Column(name = "code_hash", nullable = false, length = 128)
    val codeHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "resend_available_at", nullable = false)
    val resendAvailableAt: Instant,

    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)

@Entity
@Table(name = "account_sessions")
data class AccountSession(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
)

@Entity
@Table(
    name = "account_avatar_media",
    indexes = [Index(name = "idx_account_avatar_media_account", columnList = "account_id")]
)
data class AccountAvatarMedia(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "media_reference", nullable = false, unique = true, length = 64)
    val mediaReference: String,

    @Column(name = "account_id", nullable = false, length = 64)
    val accountId: String,

    @Column(name = "content_type", nullable = false, length = 32)
    val contentType: String,

    @Column(name = "byte_size", nullable = false)
    val byteSize: Long,

    @Column(name = "storage_name", nullable = false, unique = true, length = 128)
    val storageName: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
