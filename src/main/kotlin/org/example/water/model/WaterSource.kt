package org.example.water.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.Instant
import org.example.user.model.User
import org.example.route.model.Route

/**
 * 水源类型枚举
 */
enum class WaterSourceType(val value: Int) {
    NATURAL(0),     // 天然水源
    TREATED(1),     // 处理过的水源
    BOTTLED(2),     // 瓶装水
    OTHER(3);       // 其他

    companion object {
        fun fromValue(value: Int): WaterSourceType {
            return values().find { it.value == value } ?: OTHER
        }
    }
}

/**
 * 水质等级枚举
 */
enum class WaterQuality(val value: Int) {
    EXCELLENT(0),   // 优质
    GOOD(1),        // 良好
    FAIR(2),        // 一般
    POOR(3),        // 较差
    UNKNOWN(4);     // 未知

    companion object {
        fun fromValue(value: Int): WaterQuality {
            return values().find { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 水源实体
 */
@Entity
@Table(name = "water_sources")
data class WaterSource(
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
    
    @Column(name = "water_type", nullable = false)
    val waterType: Int = 0,
    
    @Column(name = "water_quality", nullable = false)
    val waterQuality: Int = 4,
    
    @Column(name = "requires_treatment", nullable = false)
    val requiresTreatment: Boolean = false,
    
    val reliability: Double? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    
    @Column(name = "last_verified")
    val lastVerified: String? = null,

    @Column(name = "verified_by_id", length = 64)
    val verifiedById: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id", insertable = false, updatable = false)
    var verifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
) {
    /**
     * 获取水源类型枚举
     */
    @Transient
    fun getWaterTypeEnum(): WaterSourceType {
        return WaterSourceType.fromValue(waterType)
    }

    /**
     * 获取水质等级枚举
     */
    @Transient
    fun getWaterQualityEnum(): WaterQuality {
        return WaterQuality.fromValue(waterQuality)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as WaterSource
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "WaterSource(id='$id', name='$name', waterType=${getWaterTypeEnum()}, waterQuality=${getWaterQualityEnum()})"
    }
}