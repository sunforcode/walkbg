package org.example.repository

import org.example.model.DailyPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DailyPlanRepository : JpaRepository<DailyPlan, String>