package org.example.trip.personal.service

import org.example.trip.personal.dto.CalendarDayProjection
import org.example.trip.personal.dto.PersonalTripCalendarProjection
import org.example.trip.personal.model.PersonalTripRecord
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class PersonalTripDomainService(
    private val clock: Clock = Clock.systemUTC()
) {
    data class GroupedTrips(
        val current: List<PersonalTripRecord>,
        val historical: List<PersonalTripRecord>
    )

    fun status(record: PersonalTripRecord): String {
        if (record.lifecycleState == "cancelled") return "cancelled"
        val today = businessDate()
        return when {
            today.isBefore(record.startDate) -> "planned"
            today.isAfter(record.endDate) -> "completed"
            else -> "in_progress"
        }
    }

    fun groupAndOrder(records: List<PersonalTripRecord>): GroupedTrips {
        val current = records.filter { status(it) in CURRENT_STATUSES }
            .sortedWith(
                compareBy<PersonalTripRecord> { if (status(it) == "in_progress") 0 else 1 }
                    .thenBy { it.startDate }
                    .thenComparator { left, right ->
                        if (status(left) == "in_progress" && status(right) == "in_progress") {
                            left.endDate.compareTo(right.endDate)
                        } else {
                            left.firstGeneratedAt.compareTo(right.firstGeneratedAt)
                        }
                    }
                    .thenBy { it.id }
            )
        val historical = records.filter { status(it) in HISTORICAL_STATUSES }
            .sortedWith(
                compareByDescending<PersonalTripRecord> { it.startDate }
                    .thenBy { it.firstGeneratedAt }
                    .thenBy { it.id }
            )
        return GroupedTrips(current, historical)
    }

    fun focus(records: List<PersonalTripRecord>): PersonalTripRecord? =
        groupAndOrder(records).current.firstOrNull()

    fun calendar(records: List<PersonalTripRecord>): PersonalTripCalendarProjection {
        val windowEndDate = businessDate()
        val windowStartDate = windowEndDate.minusDays(364)
        val counts = sortedMapOf<LocalDate, Int>()
        records.forEach { record ->
            val firstDate = maxOf(record.startDate, windowStartDate)
            val lastDate = minOf(record.endDate, windowEndDate)
            if (!firstDate.isAfter(lastDate)) {
                var date = firstDate
                while (!date.isAfter(lastDate)) {
                    counts[date] = (counts[date] ?: 0) + 1
                    date = date.plusDays(1)
                }
            }
        }
        return PersonalTripCalendarProjection(
            windowStartDate,
            windowEndDate,
            counts.map { (date, count) -> CalendarDayProjection(date, count) }
        )
    }

    fun businessDate(): LocalDate = LocalDate.now(clock.withZone(SHANGHAI_ZONE))

    companion object {
        private val SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai")
        private val CURRENT_STATUSES = setOf("planned", "in_progress")
        private val HISTORICAL_STATUSES = setOf("completed", "cancelled")
    }
}
