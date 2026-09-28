package org.example.equipment.preparation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.common.contract.ApiContractException
import org.example.equipment.support.EquipmentCommandStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EquipmentCommandStoreTest {
    private lateinit var jdbc: JdbcTemplate
    private lateinit var store: EquipmentCommandStore
    private lateinit var transaction: TransactionTemplate
    private val mapper = jacksonObjectMapper()

    data class Result(val identity: String, val value: Double)

    @BeforeEach
    fun setup() {
        val source = DriverManagerDataSource("jdbc:h2:mem:receipt-${UUID.randomUUID()};DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "")
        jdbc = JdbcTemplate(source)
        jdbc.execute("CREATE TABLE users (id VARCHAR(64) PRIMARY KEY)")
        jdbc.update("INSERT INTO users (id) VALUES ('account-a'), ('account-b')")
        jdbc.execute("""CREATE TABLE equipment_command_receipts (
            account_id VARCHAR(64) NOT NULL, operation_key VARCHAR(255) NOT NULL,
            idempotency_key_hash CHAR(64) NOT NULL, request_hash CHAR(64) NOT NULL,
            response_json CLOB NOT NULL, created_at TIMESTAMP NOT NULL,
            PRIMARY KEY (account_id, operation_key, idempotency_key_hash))""")
        jdbc.execute("CREATE TABLE effects (id VARCHAR(64) PRIMARY KEY)")
        transaction = TransactionTemplate(DataSourceTransactionManager(source))
        store = EquipmentCommandStore(jdbc, mapper)
    }

    @Test
    fun `equal numeric values and property order replay the original complete response`() {
        val first = transaction.execute {
            store.execute("account-a", "create", "key", mapper.readTree("""{"weight":12.50,"name":"A"}"""), Result::class.java) {
                Result("first", 12.5)
            }
        }
        val replay = transaction.execute {
            store.execute("account-a", "create", "key", mapper.readTree("""{"name":"A","weight":12.5}"""), Result::class.java) {
                fail("replay must not execute or reread the deleted source")
            }
        }
        assertEquals(first, replay)
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM equipment_command_receipts", Int::class.java))
    }

    @Test
    fun `key scope separates accounts and tasks while different requests conflict`() {
        for ((account, operation) in listOf("account-a" to "copy", "account-b" to "copy", "account-a" to "create")) {
            transaction.execute { store.execute(account, operation, "same", mapOf("n" to 1), Result::class.java) { Result("$account-$operation", 1.0) } }
        }
        val error = assertThrows(ApiContractException::class.java) {
            transaction.execute { store.execute("account-a", "copy", "same", mapOf("n" to 2), Result::class.java) { Result("wrong", 2.0) } }
        }
        assertEquals("idempotency_conflict", error.code)
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM equipment_command_receipts", Int::class.java))
    }

    @Test
    fun `failed action rolls back both business effects and receipt and can be retried`() {
        assertThrows(IllegalStateException::class.java) {
            transaction.execute {
                store.execute("account-a", "create", "retry", mapOf("n" to 1), Result::class.java) {
                    jdbc.update("INSERT INTO effects VALUES ('partial')")
                    error("action failed")
                }
            }
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM effects", Int::class.java))
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM equipment_command_receipts", Int::class.java))
        transaction.execute {
            store.execute("account-a", "create", "retry", mapOf("n" to 1), Result::class.java) {
                jdbc.update("INSERT INTO effects VALUES ('complete')")
                Result("complete", 1.0)
            }
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM effects", Int::class.java))
    }

    @Test
    fun `concurrent requests on the same key execute the action once`() {
        val start = CountDownLatch(1)
        val actions = AtomicInteger()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit<Result> {
                    assertTrue(start.await(5, TimeUnit.SECONDS))
                    transaction.execute {
                        store.execute("account-a", "copy", "parallel", mapOf("n" to 1), Result::class.java) {
                            Result("created-${actions.incrementAndGet()}", 1.0)
                        }
                    }!!
                }
            }
            start.countDown()
            assertEquals(futures[0].get(10, TimeUnit.SECONDS), futures[1].get(10, TimeUnit.SECONDS))
            assertEquals(1, actions.get())
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `missing account and empty key never run a command`() {
        val missing = assertThrows(ApiContractException::class.java) {
            transaction.execute { store.execute("absent", "copy", "key", emptyMap<String, String>(), Result::class.java) { fail("must not execute") } }
        }
        assertEquals("resource_not_found", missing.code)
        val empty = assertThrows(ApiContractException::class.java) {
            transaction.execute { store.execute("account-a", "copy", " ", emptyMap<String, String>(), Result::class.java) { fail("must not execute") } }
        }
        assertEquals("invalid_request", empty.code)
    }
}
