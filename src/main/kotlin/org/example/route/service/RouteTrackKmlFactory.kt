package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.model.RouteCurrentPublicVersion
import org.example.route.model.RouteVersion
import org.example.route.repository.RouteCurrentPublicVersionRepository
import org.example.route.repository.RouteVersionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 路线主轨迹点 → 最小合法 KML 合成器
 *
 * 用于重新分析时原始 KML 文件已丢失的回退：将 route_versions 的有效主轨迹点
 * （main_track_json，格式 [[lat, lng, ele?], ...]）合成为只含轨迹线的 KML 内容。
 *
 * 注意：合成的 KML 不含任何标记点（Placemark），因此该来源的分析结果不会产生
 * POI 识别点（Agent 的 POI 识别仅基于 KML 标记点）。
 */
@Service
class RouteTrackKmlFactory(
    private val currentVersionRepository: RouteCurrentPublicVersionRepository,
    private val versionRepository: RouteVersionRepository
) {
    private val logger = LoggerFactory.getLogger(RouteTrackKmlFactory::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * 解析路线当前公开版本的有效主轨迹并合成 KML。
     * 无当前版本、主轨迹无效（availability != valid）或轨迹点不足时返回 null。
     */
    fun synthesizeCurrentTrackKml(routeId: String): String? {
        val current = currentVersionRepository.findById(routeId).orElse(null) ?: return null
        val version = versionRepository.findById(current.routeVersionId).orElse(null) ?: return null
        if (version.mainTrackAvailability != "valid") return null
        val trackJson = version.mainTrackJson?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        return synthesizeKml(trackJson)
    }

    /**
     * 将 [[lat, lng, ele?], ...] 轨迹点 JSON 合成为 KML LineString 内容。
     * KML coordinates 约定为 lng,lat[,ele] 顺序。逐点宽松解析：格式错误的行被剔除，
     * 剩余有效点少于 2 个时无法构成轨迹线，返回 null。数值按源文本输出，避免
     * 整数海拔被浮点化（如 1500 → 1500.0）。
     */
    fun synthesizeKml(trackJson: String): String? {
        val root = try {
            objectMapper.readTree(trackJson)
        } catch (_: Exception) {
            return null
        }
        if (!root.isArray) return null
        val coordinates = root.mapNotNull { row ->
            if (!row.isArray || row.size() < 2) return@mapNotNull null
            val latNode = row.get(0)
            val lngNode = row.get(1)
            if (latNode == null || lngNode == null || !latNode.isNumber || !lngNode.isNumber) {
                return@mapNotNull null
            }
            val latitude = latNode.asDouble()
            val longitude = lngNode.asDouble()
            if (!latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0
            ) {
                return@mapNotNull null
            }
            val elevationNode = row.get(2)?.takeIf { it.isNumber && it.asDouble().isFinite() }
            if (elevationNode != null) "${lngNode.asText()},${latNode.asText()},${elevationNode.asText()}"
            else "${lngNode.asText()},${latNode.asText()}"
        }
        if (coordinates.size < 2) return null

        logger.info("由已存轨迹点合成 KML，共 {} 个轨迹点", coordinates.size)
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
            append("  <Document>\n")
            append("    <Placemark>\n")
            append("      <LineString>\n")
            append("        <coordinates>").append(coordinates.joinToString(" ")).append("</coordinates>\n")
            append("      </LineString>\n")
            append("    </Placemark>\n")
            append("  </Document>\n")
            append("</kml>")
        }
    }
}
