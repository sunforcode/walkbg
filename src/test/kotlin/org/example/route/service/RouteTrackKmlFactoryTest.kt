package org.example.route.service

import org.example.route.model.RouteCurrentPublicVersion
import org.example.route.model.RouteVersion
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class RouteTrackKmlFactoryTest {
    private val currentVersionRepository = mock<RouteCurrentPublicVersionRepository>()
    private val versionRepository = mock<RouteVersionRepository>()
    private val factory = RouteTrackKmlFactory(currentVersionRepository, versionRepository)

    @Test
    fun `synthesizes minimal kml linestring from track points`() {
        val kml = factory.synthesizeKml("[[39.0, 113.0, 1500], [39.001, 113.002, 1510]]")

        assertTrue(kml!!.contains("<kml"))
        assertTrue(kml.contains("<LineString>"))
        // KML coordinates 约定为 lng,lat[,ele] 顺序，与数据库 [lat, lng, ele] 相反
        assertTrue(kml.contains("113.0,39.0,1500 113.002,39.001,1510"))
        assertFalse(kml.contains("<Point>"))
    }

    @Test
    fun `synthesizes kml without elevation when track points omit it`() {
        val kml = factory.synthesizeKml("[[39.0, 113.0], [39.001, 113.002]]")

        assertTrue(kml!!.contains("113.0,39.0 113.002,39.001"))
    }

    @Test
    fun `returns null for blank or invalid track json`() {
        assertNull(factory.synthesizeKml(""))
        assertNull(factory.synthesizeKml("   "))
        assertNull(factory.synthesizeKml("not-json"))
        assertNull(factory.synthesizeKml("[]"))
    }

    @Test
    fun `skips invalid points and rejects when fewer than two remain`() {
        // 纬度越界的点被剔除后只剩 1 个有效点，不足以构成轨迹线
        assertNull(factory.synthesizeKml("[[95.0, 113.0], [39.001, 113.002]]"))
        assertNull(factory.synthesizeKml("[[39.0, 113.0]]"))
    }

    @Test
    fun `skips malformed rows but keeps valid ones`() {
        val kml = factory.synthesizeKml("[[39.0, 113.0, 1500], [\"bad\"], [39.001, 113.002, 1510]]")

        assertTrue(kml!!.contains("113.0,39.0,1500 113.002,39.001,1510"))
    }

    @Test
    fun `resolves current version track and synthesizes kml for route`() {
        val version = RouteVersion(
            id = "version-1",
            routeId = "route-1",
            routeType = null,
            mainTrackAvailability = "valid",
            mainTrackJson = "[[39.0, 113.0, 1500], [39.001, 113.002, 1510]]"
        )
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion(routeId = "route-1", routeVersionId = "version-1")))
        whenever(versionRepository.findById("version-1")).thenReturn(Optional.of(version))

        val kml = factory.synthesizeCurrentTrackKml("route-1")

        assertTrue(kml!!.contains("<LineString>"))
        assertTrue(kml.contains("113.0,39.0,1500"))
    }

    @Test
    fun `returns null when route has no current version or invalid track`() {
        whenever(currentVersionRepository.findById("route-1")).thenReturn(Optional.empty())

        assertNull(factory.synthesizeCurrentTrackKml("route-1"))
    }

    @Test
    fun `returns null when current version track availability is not valid`() {
        val version = RouteVersion(
            id = "version-1",
            routeId = "route-1",
            routeType = null,
            mainTrackAvailability = "missing",
            mainTrackJson = "[[39.0, 113.0, 1500], [39.001, 113.002, 1510]]"
        )
        whenever(currentVersionRepository.findById("route-1"))
            .thenReturn(Optional.of(RouteCurrentPublicVersion(routeId = "route-1", routeVersionId = "version-1")))
        whenever(versionRepository.findById("version-1")).thenReturn(Optional.of(version))

        assertNull(factory.synthesizeCurrentTrackKml("route-1"))
    }

    @Test
    fun `synthesized kml coordinate precision keeps full double value`() {
        val kml = factory.synthesizeKml("[[31.720927, 103.09661, 2596], [31.721844, 103.094678, 2631]]")

        assertEquals(true, kml!!.contains("103.09661,31.720927,2596 103.094678,31.721844,2631"))
    }
}
