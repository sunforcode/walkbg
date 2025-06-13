package org.example.route.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.example.route.model.DailyPlan

@Repository
interface DailyPlanRepository : JpaRepository<DailyPlan, String>