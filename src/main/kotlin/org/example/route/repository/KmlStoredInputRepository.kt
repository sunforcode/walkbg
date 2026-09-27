package org.example.route.repository

import org.example.route.model.KmlStoredInput
import org.springframework.data.jpa.repository.JpaRepository

interface KmlStoredInputRepository : JpaRepository<KmlStoredInput, String>
