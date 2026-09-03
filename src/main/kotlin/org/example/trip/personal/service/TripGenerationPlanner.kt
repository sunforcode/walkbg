package org.example.trip.personal.service

import org.example.route.model.RouteVersion
import org.example.trip.personal.dto.GenerateTripCommand
import org.example.trip.personal.dto.TransportOptionProjection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

interface TripGenerationPlanner {
    fun plan(input: TripPlanningInput): TripPlanningDecision
}

data class TripPlanningInput(
    val accountId: String,
    val routeVersion: RouteVersion,
    val command: GenerateTripCommand,
    val transportOptionId: String? = command.transportSelection?.transportOptionId
)

sealed interface TripPlanningDecision {
    data object Ready : TripPlanningDecision

    data class SelectionRequired(
        val selectionId: String,
        val options: List<TransportOptionProjection>
    ) : TripPlanningDecision
}

object DirectTripGenerationPlanner : TripGenerationPlanner {
    override fun plan(input: TripPlanningInput): TripPlanningDecision = TripPlanningDecision.Ready
}

@Configuration
class TripGenerationPlannerConfiguration {
    @Bean
    fun tripGenerationPlanner(): TripGenerationPlanner = DirectTripGenerationPlanner
}
