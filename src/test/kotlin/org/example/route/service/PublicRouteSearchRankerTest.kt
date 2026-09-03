package org.example.route.service

import org.example.route.dto.PublicRouteSearchSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PublicRouteSearchRankerTest {

    @Test
    fun `search ranks six match levels and preserves configured order within a level`() {
        val candidates = listOf(
            candidate("region-exact", name = "海岸线", region = "贡嘎"),
            candidate("name-contains", name = "徒步贡嘎环线", region = "四川"),
            candidate("name-prefix-first", name = "贡嘎东线", region = "四川"),
            candidate("name-exact-first", name = "贡嘎", region = "四川"),
            candidate("name-exact-second", name = "贡嘎", region = "云南"),
            candidate("name-prefix-second", name = "贡嘎西线", region = "四川")
        )

        val results = PublicRouteSearchRanker.search("贡嘎", candidates)

        assertEquals(
            listOf(
                "name-exact-first",
                "name-exact-second",
                "name-prefix-first",
                "name-prefix-second",
                "name-contains",
                "region-exact"
            ),
            results.map { it.routeId }
        )
    }

    @Test
    fun `search normalizes only defined spaces full-width ascii and ascii case`() {
        val candidates = listOf(
            candidate("match", name = "ABC  Route", region = null),
            candidate("non-match", name = "ＡＢＤ Route", region = null)
        )

        val results = PublicRouteSearchRanker.search("\tＡｂｃ　 Route\r\n", candidates)

        assertEquals(listOf("match"), results.map { it.routeId })
    }

    private fun candidate(routeId: String, name: String?, region: String?) =
        PublicRouteSearchSummary(
            routeId = routeId,
            currentVersionId = "$routeId-version",
            name = name,
            region = region
        )
}
