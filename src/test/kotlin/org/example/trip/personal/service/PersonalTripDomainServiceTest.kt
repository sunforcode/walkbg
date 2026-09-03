package org.example.trip.personal.service

import org.example.trip.personal.model.PersonalTripRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PersonalTripDomainServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-01T04:00:00Z"), ZoneId.of("UTC"))
    private val service = PersonalTripDomainService(clock)

    @Test
    fun `focus is the first current trip in authoritative order`() {
        val records = listOf(
            trip("planned-later", "2026-09-03", "2026-09-03", "2026-01-01T00:00:02Z"),
            trip("active-later-end", "2026-08-31", "2026-09-04", "2026-01-01T00:00:01Z"),
            trip("active-earlier-end", "2026-08-31", "2026-09-02", "2026-01-01T00:00:03Z")
        )

        val ordered = service.groupAndOrder(records)

        assertEquals(listOf("active-earlier-end", "active-later-end", "planned-later"), ordered.current.map { it.id })
        assertEquals(ordered.current.first().id, service.focus(records)?.id)
    }

    @Test
    fun `history is sorted by start date descending then first generation ascending`() {
        val records = listOf(
            trip("same-day-later", "2026-08-01", "2026-08-02", "2026-01-02T00:00:00Z"),
            trip("newer-date", "2026-08-15", "2026-08-16", "2026-01-03T00:00:00Z"),
            trip("same-day-earlier", "2026-08-01", "2026-08-03", "2026-01-01T00:00:00Z", cancelled = true)
        )

        assertEquals(
            listOf("newer-date", "same-day-earlier", "same-day-later"),
            service.groupAndOrder(records).historical.map { it.id }
        )
    }

    @Test
    fun `empty collection has no focus and calendar counts intersecting trips`() {
        assertNull(service.focus(emptyList()))
        val calendar = service.calendar(
            listOf(
                trip("first", "2026-08-31", "2026-09-02", "2026-01-01T00:00:00Z"),
                trip("second", "2026-09-01", "2026-09-01", "2026-01-02T00:00:00Z")
            )
        )

        assertEquals(LocalDate.parse("2025-09-02"), calendar.windowStartDate)
        assertEquals(LocalDate.parse("2026-09-01"), calendar.windowEndDate)
        assertEquals(
            listOf("2026-08-31" to 1, "2026-09-01" to 2),
            calendar.days.map { it.date.toString() to it.tripCount }
        )
    }

    private fun trip(
        id: String,
        start: String,
        end: String,
        firstGeneratedAt: String,
        cancelled: Boolean = false
    ) = PersonalTripRecord(
        id = id,
        name = id,
        firstGeneratedAt = Instant.parse(firstGeneratedAt),
        lifecycleState = if (cancelled) "cancelled" else "active",
        departureCity = "上海",
        startDate = LocalDate.parse(start),
        endDate = LocalDate.parse(end),
        totalDayCount = 1,
        hikingDayCount = 1,
        revision = "revision-$id",
        frozenRouteBasisJson = "{}"
    )
}
