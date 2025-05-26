package org.example.repository

import org.example.model.Waypoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WaypointRepository : JpaRepository<Waypoint, String>