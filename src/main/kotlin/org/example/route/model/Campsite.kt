package org.example.route.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import org.example.user.model.User
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
 * 营地实体（单向关联）
 */
@Entity
@Table(
    name = "campsites",
    indexes = [
        Index(name = "idx_campsites_route_id", columnList = "route_id"),
        Index(name = "idx_campsites_type", columnList = "campsite_type"),
        Index(name = "idx_campsites_created_by", columnList = "created_by")
    ]
)
data class Campsite(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "route_id", length = 64, nullable = false)
    val routeId: String,

    @Column(nullable = false, length = 200)
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

    @Column(name = "last_verified_id", length = 64)
    val lastVerifiedId: String? = null,

    @Column(name = "created_by", length = 64)
    var createdBy: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
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