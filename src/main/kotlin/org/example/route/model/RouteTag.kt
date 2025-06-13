package org.example.route.model

import jakarta.persistence.*

/**
 * 路线标签实体
 */
@Entity
@Table(
    name = "route_tags",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["route_id", "tag"])
    ],
    indexes = [
        Index(name = "idx_route_tags_route_id", columnList = "route_id"),
        Index(name = "idx_route_tags_tag", columnList = "tag")
    ]
)
data class RouteTag(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(nullable = false, length = 50)
    val tag: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteTag

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "RouteTag(id='$id', tag='$tag')"
    }
}
