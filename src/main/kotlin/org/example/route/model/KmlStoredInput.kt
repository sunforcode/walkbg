package org.example.route.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Durable original input. The static file is only a compatibility copy. */
@Entity
@Table(name = "kml_stored_inputs")
class KmlStoredInput(
    @Id
    @Column(name = "kml_url", length = 500)
    val kmlUrl: String,

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    val content: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
