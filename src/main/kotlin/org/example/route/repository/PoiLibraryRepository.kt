package org.example.route.repository

import org.example.route.model.PoiLibraryItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PoiLibraryRepository : JpaRepository<PoiLibraryItem, String> {
    fun findByStatus(status: String): List<PoiLibraryItem>
    fun findByName(name: String): List<PoiLibraryItem>
}
