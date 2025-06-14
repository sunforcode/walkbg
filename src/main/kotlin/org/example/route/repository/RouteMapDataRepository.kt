package org.example.route.repository

import org.example.route.model.RouteMapData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RouteMapDataRepository : JpaRepository<RouteMapData, String>
