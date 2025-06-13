package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant

/**
 * 营地类型枚举
 */
enum class CampsiteType(val value: Int) {
    OFFICIAL(0),    // 官方指定
    UNOFFICIAL(1),  // 非官方
    WILD(2),        // 野营
    SHELTER(3),     // 避难所
    OTHER(4);       // 其他

    companion object {
        fun fromValue(value: Int): CampsiteType {
            return values().find { it.value == value } ?: OTHER
        }
    }
}

/**
 * 营地实体
 */
@Entity
@Table(name = "campsites")
data class Campsite(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,

    @Column(name = "campsite_type", nullable = false)
    val campsiteType: Int = 0,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "last_verified_id", length = 64)
    val lastVerifiedId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
) {
    /**
     * 获取营地类型枚举
     */
    @Transient
    fun getCampsiteTypeEnum(): CampsiteType {
        return CampsiteType.fromValue(campsiteType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Campsite

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Campsite(id='$id', name='$name', campsiteType=${getCampsiteTypeEnum()})"
    }
}