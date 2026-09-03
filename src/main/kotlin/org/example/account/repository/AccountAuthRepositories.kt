package org.example.account.repository

import org.example.account.model.AccountAvatarMedia
import org.example.account.model.AccountSession
import org.example.account.model.AccountVerification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

interface AccountVerificationRepository : JpaRepository<AccountVerification, String> {
    fun findFirstByPhoneOrderByCreatedAtDesc(phone: String): AccountVerification?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from AccountVerification verification where verification.id = :id")
    fun findByIdForUpdate(@Param("id") id: String): AccountVerification?
}

interface AccountSessionRepository : JpaRepository<AccountSession, String> {
    fun existsByIdAndAccountIdAndRevokedAtIsNull(id: String, accountId: String): Boolean
}

interface AccountAvatarMediaRepository : JpaRepository<AccountAvatarMedia, String> {
    fun existsByMediaReferenceAndAccountId(mediaReference: String, accountId: String): Boolean
}
