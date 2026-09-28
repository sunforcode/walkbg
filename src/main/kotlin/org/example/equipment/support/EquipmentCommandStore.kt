package org.example.equipment.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.contract.ApiContractException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Instant
import java.util.HexFormat

/** Account lock and first-result receipts share the caller's business transaction. */
@Component
class EquipmentCommandStore(
    private val jdbc: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun lockAccount(accountId: String) {
        val rows = jdbc.query("SELECT id FROM users WHERE id = ? FOR UPDATE", { rs, _ -> rs.getString(1) }, accountId)
        if (rows.isEmpty()) throw ApiContractException.notFound()
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun <T : Any> execute(
        accountId: String,
        operation: String,
        idempotencyKey: String,
        request: Any,
        responseType: Class<T>,
        action: () -> T
    ): T {
        if (idempotencyKey.isBlank()) throw ApiContractException.invalidRequest("Idempotency-Key 不能为空")
        lockAccount(accountId)
        val keyHash = hash(idempotencyKey)
        val requestHash = hash(canonical(objectMapper.valueToTree(request)))
        val existing = jdbc.query(
            """SELECT request_hash, response_json FROM equipment_command_receipts
                WHERE account_id = ? AND operation_key = ? AND idempotency_key_hash = ?""",
            { rs, _ -> rs.getString(1) to rs.getString(2) }, accountId, operation, keyHash
        ).singleOrNull()
        if (existing != null) {
            if (existing.first != requestHash) {
                throw ApiContractException.conflict("idempotency_conflict", "幂等键已用于不同请求")
            }
            return objectMapper.readValue(existing.second, responseType)
        }
        val result = action()
        jdbc.update(
            """INSERT INTO equipment_command_receipts
                (account_id, operation_key, idempotency_key_hash, request_hash, response_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?)""",
            accountId, operation, keyHash, requestHash, objectMapper.writeValueAsString(result), Timestamp.from(Instant.now())
        )
        return result
    }

    private fun canonical(node: JsonNode): String = when {
        node.isObject -> node.fieldNames().asSequence().sorted().joinToString(",", "{", "}") { key ->
            objectMapper.writeValueAsString(key) + ":" + canonical(node.get(key))
        }
        node.isArray -> node.joinToString(",", "[", "]", transform = ::canonical)
        node.isNumber -> node.decimalValue().stripTrailingZeros().toPlainString()
        else -> objectMapper.writeValueAsString(node)
    }

    private fun hash(value: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
    )
}
