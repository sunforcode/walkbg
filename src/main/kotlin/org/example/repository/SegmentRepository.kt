package org.example.repository

import org.example.model.Segment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SegmentRepository : JpaRepository<Segment, String>