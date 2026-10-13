package org.example.scene

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.persistence.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SceneImage(val mediaId: String, val imageUrl: String, val width: Int, val height: Int, val kind: String = "aigc")
data class RouteDayScene(val referenceDayId: String, val image: SceneImage)
data class TripDayScene(val tripDayId: String, val image: SceneImage)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RouteSceneProjection(val routeId: String, val routeVersionId: String, val sceneRevision: String, val overview: SceneImage? = null, val days: List<RouteDayScene> = emptyList())
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TripSceneProjection(val tripId: String, val tripRevision: String, val routeVersionId: String, val sceneRevision: String, val overview: SceneImage? = null, val days: List<TripDayScene> = emptyList())
data class ManagedSceneProjection<T>(val revision: Long, val projection: T)
data class SceneDayBinding(val dayId: String, val mediaId: String)
data class SceneBindings(val overviewMediaId: String? = null, val days: List<SceneDayBinding> = emptyList())
data class ReplaceRouteScenes(val expectedRevision: Long, val bindings: SceneBindings)
data class ReplaceTripScenes(val expectedRevision: Long, val tripRevision: String, val routeVersionId: String, val bindings: SceneBindings)

@Entity
@Table(name = "scene_media")
class SceneMedia(
    @Id @Column(name = "media_id", length = 64) val mediaId: String,
    @Column(name = "content_type", nullable = false, length = 32) val contentType: String,
    @Column(nullable = false) val width: Int,
    @Column(nullable = false) val height: Int,
    @Lob @Column(name = "media_bytes", nullable = false, columnDefinition = "LONGBLOB") val bytes: ByteArray
)

@Entity
@Table(name = "route_scene_sets")
class RouteSceneSet(
    @Id @Column(name = "route_version_id", length = 64) val routeVersionId: String,
    @Column(name = "route_id", nullable = false, length = 64) val routeId: String,
    @Column(nullable = false) var revision: Long,
    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT") var contentJson: String
)

@Entity
@Table(name = "trip_scene_sets")
class TripSceneSet(
    @Id @Column(name = "trip_id", length = 64) val tripId: String,
    @Column(name = "route_version_id", nullable = false, length = 64) var routeVersionId: String,
    @Column(name = "trip_revision", nullable = false, length = 64) var tripRevision: String,
    @Column(nullable = false) var revision: Long,
    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT") var contentJson: String
)
