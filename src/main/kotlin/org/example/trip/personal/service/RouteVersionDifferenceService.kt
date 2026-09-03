package org.example.trip.personal.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.model.RouteVersion
import org.example.route.model.RouteVersionEquipmentSuggestion
import org.example.route.model.RouteVersionSegment
import org.example.trip.personal.dto.QualifiedValueProjection
import org.example.trip.personal.dto.RouteVersionDifferenceItem
import org.example.trip.personal.dto.RouteVersionDifferenceProjection
import org.springframework.stereotype.Service

@Service
class RouteVersionDifferenceService(
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
) {
    fun compare(
        before: RouteVersion,
        after: RouteVersion,
        beforeSegments: List<RouteVersionSegment>,
        afterSegments: List<RouteVersionSegment>,
        beforeSuggestions: List<RouteVersionEquipmentSuggestion>,
        afterSuggestions: List<RouteVersionEquipmentSuggestion>
    ): RouteVersionDifferenceProjection {
        val differences = mutableListOf<RouteVersionDifferenceItem>()

        compareText(differences, IDENTITY, "route_name", "路线名称", before.name, after.name)
        compareText(differences, IDENTITY, "region", "所在地区", before.region, after.region)
        compareText(differences, IDENTITY, "route_type", "路线类型", before.routeType, after.routeType)
        compareText(differences, IDENTITY, "direction", "路线走法", before.direction, after.direction)
        compareText(differences, IDENTITY, "start", "路线起点", before.startName, after.startName)
        compareText(differences, IDENTITY, "end", "路线终点", before.endName, after.endName)

        compareText(
            differences,
            TRACK,
            "main_track_availability",
            "主轨迹状态",
            before.mainTrackAvailability,
            after.mainTrackAvailability
        )
        compareText(
            differences,
            TRACK,
            "main_track",
            "主轨迹",
            canonicalTrack(before),
            canonicalTrack(after)
        )
        compareText(
            differences,
            TRACK,
            "segments",
            "分段结构",
            canonicalSegments(beforeSegments),
            canonicalSegments(afterSegments)
        )

        compareText(differences, SCALE, "distance", "距离", before.distanceMeters?.stripTrailingZeros()?.toPlainString(), after.distanceMeters?.stripTrailingZeros()?.toPlainString())
        compareText(differences, SCALE, "ascent", "累计爬升", before.ascentMeters?.stripTrailingZeros()?.toPlainString(), after.ascentMeters?.stripTrailingZeros()?.toPlainString())
        compareText(differences, SCALE, "descent", "累计下降", before.descentMeters?.stripTrailingZeros()?.toPlainString(), after.descentMeters?.stripTrailingZeros()?.toPlainString())
        compareText(differences, SCALE, "estimated_duration", "预计用时", before.estimatedDurationSeconds?.toString(), after.estimatedDurationSeconds?.toString())
        compareText(differences, SCALE, "difficulty", "难度", before.difficulty, after.difficulty)

        compareSuggestions(differences, beforeSuggestions, afterSuggestions)

        return RouteVersionDifferenceProjection(
            minimumComparisonStatus = "partial",
            differences = differences,
            unavailableCategories = listOf(PLANNING),
            otherRelevantChangeStatus = "none",
            otherRelevantChanges = emptyList()
        )
    }

    private fun canonicalTrack(version: RouteVersion): String? {
        if (version.mainTrackAvailability != "valid") return null
        val referenceSystem = version.mainTrackReferenceSystem?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val tree = version.mainTrackJson?.let { runCatching { objectMapper.readTree(it) }.getOrNull() } ?: return null
        if (!tree.isArray || tree.isEmpty) return null
        return objectMapper.writeValueAsString(mapOf("reference_system" to referenceSystem, "path" to tree))
    }

    private fun canonicalSegments(segments: List<RouteVersionSegment>): String? =
        segments.takeIf { it.isNotEmpty() }?.let { values ->
            objectMapper.writeValueAsString(values.sortedBy { it.segmentOrder }.map { segment ->
                mapOf(
                    "order" to segment.segmentOrder,
                    "name" to segment.name,
                    "start" to segment.startName,
                    "end" to segment.endName,
                    "distance" to segment.distanceMeters?.stripTrailingZeros()?.toPlainString(),
                    "duration" to segment.estimatedDurationSeconds,
                    "ascent" to segment.ascentMeters?.stripTrailingZeros()?.toPlainString(),
                    "descent" to segment.descentMeters?.stripTrailingZeros()?.toPlainString(),
                    "difficulty" to segment.difficulty,
                    "terrain" to segment.terrainOrRoadType,
                    "description" to segment.description,
                    "notes" to segment.notes
                )
            })
        }

    private fun compareSuggestions(
        destination: MutableList<RouteVersionDifferenceItem>,
        before: List<RouteVersionEquipmentSuggestion>,
        after: List<RouteVersionEquipmentSuggestion>
    ) {
        val beforeByLogical = before.associateBy { it.logicalSuggestionId }
        val afterByLogical = after.associateBy { it.logicalSuggestionId }
        val orderedLogicalIds = buildList {
            after.sortedBy { it.displayOrder }.forEach { add(it.logicalSuggestionId) }
            before.sortedBy { it.displayOrder }.forEach { if (it.logicalSuggestionId !in this) add(it.logicalSuggestionId) }
        }
        orderedLogicalIds.forEach { logicalId ->
            val old = beforeByLogical[logicalId]
            val new = afterByLogical[logicalId]
            val oldValue = old?.let(::suggestionValue)
            val newValue = new?.let(::suggestionValue)
            compareText(
                destination,
                EQUIPMENT,
                logicalId,
                when {
                    old == null -> "新增装备建议"
                    new == null -> "不再提供装备建议"
                    else -> "装备建议"
                },
                oldValue,
                newValue
            )
        }
    }

    private fun suggestionValue(suggestion: RouteVersionEquipmentSuggestion): String = buildString {
        append(suggestion.name)
        append(" × ")
        append(suggestion.quantity)
        suggestion.unitWeightGrams?.let { append("，单件 ${it}g") }
        append("，")
        append(if (suggestion.level == "required") "必需" else "建议")
        suggestion.note?.let { append("，$it") }
    }

    private fun compareText(
        destination: MutableList<RouteVersionDifferenceItem>,
        category: String,
        subject: String,
        label: String,
        before: String?,
        after: String?
    ) {
        if (before == after) return
        val changeType = when {
            before == null -> "added"
            after == null -> "no_longer_provided"
            else -> "modified"
        }
        destination += RouteVersionDifferenceItem(
            category = category,
            changeType = changeType,
            subject = subject,
            summary = when (changeType) {
                "added" -> "$label 已新增"
                "no_longer_provided" -> "$label 不再提供"
                else -> "$label 已变化"
            },
            before = before?.let { QualifiedValueProjection(value = it) },
            after = after?.let { QualifiedValueProjection(value = it) }
        )
    }

    private companion object {
        const val IDENTITY = "identity_and_structure"
        const val TRACK = "track_and_segments"
        const val SCALE = "scale_difficulty_and_time"
        const val PLANNING = "planning_support_and_safety"
        const val EQUIPMENT = "seasonal_preparation_and_equipment"
    }
}
