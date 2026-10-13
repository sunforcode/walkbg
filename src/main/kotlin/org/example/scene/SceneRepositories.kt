package org.example.scene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SceneMediaMetadata {
    val mediaId: String
    val contentType: String
    val width: Int
    val height: Int
}
interface SceneMediaRepository : JpaRepository<SceneMedia, String> {
    // Only the content-addressed key is touched on a duplicate; existing bytes are immutable.
    @Modifying
    @Query(value = "INSERT INTO scene_media (media_id, content_type, width, height, media_bytes) VALUES (:id, :type, :width, :height, :bytes) ON DUPLICATE KEY UPDATE media_id = media_id", nativeQuery = true)
    fun insertImmutable(@Param("id") id: String, @Param("type") type: String, @Param("width") width: Int, @Param("height") height: Int, @Param("bytes") bytes: ByteArray): Int

    @Query("select m.mediaId as mediaId, m.contentType as contentType, m.width as width, m.height as height from SceneMedia m where m.mediaId = :id")
    fun metadata(@Param("id") id: String): SceneMediaMetadata?
}
interface RouteSceneSetRepository : JpaRepository<RouteSceneSet, String>
interface TripSceneSetRepository : JpaRepository<TripSceneSet, String>
