package org.example.route.service

import org.example.route.dto.PublicRouteSearchSummary

object PublicRouteSearchRanker {
    fun search(query: String?, candidates: List<PublicRouteSearchSummary>): List<PublicRouteSearchSummary> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        return candidates.mapIndexedNotNull { index, candidate ->
            matchLevel(normalizedQuery, candidate)?.let { level -> Ranked(candidate, level, index) }
        }.sortedWith(compareBy<Ranked> { it.level }.thenBy { it.configuredOrder })
            .map { it.route }
    }

    fun normalize(value: String?): String {
        if (value == null) return ""
        val result = StringBuilder(value.length)
        var previousWasSpace = true

        value.forEach { source ->
            val widthNormalized = when {
                source in '\uFF01'..'\uFF5E' -> (source.code - 0xFEE0).toChar()
                source == '\t' || source == '\n' || source == '\r' || source == ' ' || source == '\u3000' -> ' '
                else -> source
            }
            val mapped = if (widthNormalized in 'A'..'Z') {
                (widthNormalized.code + ('a'.code - 'A'.code)).toChar()
            } else {
                widthNormalized
            }
            if (mapped == ' ') {
                if (!previousWasSpace) result.append(' ')
                previousWasSpace = true
            } else {
                result.append(mapped)
                previousWasSpace = false
            }
        }

        if (result.isNotEmpty() && result.last() == ' ') result.deleteCharAt(result.lastIndex)
        return result.toString()
    }

    private fun matchLevel(query: String, candidate: PublicRouteSearchSummary): Int? {
        val name = normalize(candidate.name)
        val region = normalize(candidate.region)
        return when {
            name == query -> 1
            name.startsWith(query) -> 2
            name.contains(query) -> 3
            region == query -> 4
            region.startsWith(query) -> 5
            region.contains(query) -> 6
            else -> null
        }
    }

    private data class Ranked(
        val route: PublicRouteSearchSummary,
        val level: Int,
        val configuredOrder: Int
    )
}
