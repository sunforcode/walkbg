package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.example.route.model.PoiLibraryItem

// =========================================================================
// Agent 交互 DTO（snake_case 对齐 kml-agent-service）
// =========================================================================

/** 发给 Agent 的 POI 筛选请求 */
data class PoiFilterAgentRequest(
    @JsonProperty("route_id") val routeId: String?,
    @JsonProperty("pois") val pois: List<PoiFilterAgentItem>
)

data class PoiFilterAgentItem(
    @JsonProperty("name") val name: String,
    @JsonProperty("category") val category: String?,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("elevation") val elevation: Double?,
    @JsonProperty("description") val description: String?
)

/** Agent POI 筛选响应 */
data class PoiFilterAgentResponse(
    @JsonProperty("total") val total: Int,
    @JsonProperty("keep_count") val keepCount: Int,
    @JsonProperty("reject_count") val rejectCount: Int,
    @JsonProperty("degraded") val degraded: Boolean = false,
    @JsonProperty("results") val results: List<PoiFilterAgentResultItem> = emptyList()
)

data class PoiFilterAgentResultItem(
    @JsonProperty("index") val index: Int,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("latitude") val latitude: Double? = null,
    @JsonProperty("longitude") val longitude: Double? = null,
    @JsonProperty("action") val action: String = "keep",
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("original_category") val originalCategory: String? = null,
    @JsonProperty("reason") val reason: String? = null
)

/** 发给 Agent 的 POI 位置合并判定请求 */
data class PoiResolveAgentRequest(
    @JsonProperty("route_id") val routeId: String?,
    @JsonProperty("pois") val pois: List<PoiResolveAgentPoi>,
    @JsonProperty("library") val library: List<PoiResolveAgentLibraryItem>
)

data class PoiResolveAgentPoi(
    @JsonProperty("name") val name: String,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("elevation") val elevation: Double? = null,
    @JsonProperty("exclude_id") val excludeId: String? = null
)

data class PoiResolveAgentLibraryItem(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("elevation") val elevation: Double? = null
)

/** Agent POI 位置判定响应 */
data class PoiResolveAgentResponse(
    @JsonProperty("total") val total: Int,
    @JsonProperty("matched_count") val matchedCount: Int,
    @JsonProperty("degraded") val degraded: Boolean = false,
    @JsonProperty("results") val results: List<PoiResolveAgentResultItem> = emptyList()
)

data class PoiResolveAgentResultItem(
    @JsonProperty("index") val index: Int,
    @JsonProperty("library_id") val libraryId: String? = null,
    @JsonProperty("reason") val reason: String? = null
)

// =========================================================================
// 前端交互 DTO
// =========================================================================

/** POI 筛选预览请求 */
data class PoiFilterPreviewRequest(
    @JsonProperty("route_id") val routeId: String
)

/** 筛选预览单行（含路线 POI ID，便于确认后回写状态） */
data class PoiFilterPreviewItem(
    @JsonProperty("poi_id") val poiId: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("elevation") val elevation: Double?,
    @JsonProperty("action") val action: String,
    @JsonProperty("category") val category: String?,
    @JsonProperty("original_category") val originalCategory: String?,
    @JsonProperty("reason") val reason: String?,
    @JsonProperty("sub_category") val subCategory: String?,
    @JsonProperty("description") val description: String?
)

/** POI 筛选预览响应 */
data class PoiFilterPreviewResponse(
    @JsonProperty("route_id") val routeId: String,
    @JsonProperty("total") val total: Int,
    @JsonProperty("keep_count") val keepCount: Int,
    @JsonProperty("reject_count") val rejectCount: Int,
    @JsonProperty("degraded") val degraded: Boolean,
    @JsonProperty("items") val items: List<PoiFilterPreviewItem>
)

/** 确认入库的单条 POI */
data class PoiLibrarySaveItem(
    @JsonProperty("poi_id") val poiId: String?,
    @JsonProperty("name") val name: String,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("elevation") val elevation: Double?,
    @JsonProperty("category") val category: String,
    @JsonProperty("sub_category") val subCategory: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("ai_reason") val aiReason: String? = null
)

/** POI 入库请求 */
data class PoiLibrarySaveRequest(
    @JsonProperty("route_id") val routeId: String,
    @JsonProperty("items") val items: List<PoiLibrarySaveItem>
)

/** POI 入库响应统计 */
data class PoiLibrarySaveResponse(
    @JsonProperty("saved") val saved: Int,
    @JsonProperty("updated") val updated: Int,
    @JsonProperty("confirmed_pois") val confirmedPois: Int
)

/** 库条目响应 */
data class PoiLibraryItemDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("elevation") val elevation: Double?,
    @JsonProperty("category") val category: String,
    @JsonProperty("region_id") val regionId: String?,
    @JsonProperty("region_name") val regionName: String?,
    @JsonProperty("sub_category") val subCategory: String?,
    @JsonProperty("description") val description: String?,
    @JsonProperty("ai_reason") val aiReason: String?,
    @JsonProperty("source_route_id") val sourceRouteId: String?,
    @JsonProperty("status") val status: String,
    @JsonProperty("created_at") val createdAt: Any?
) {
    companion object {
        fun fromPoiLibraryItem(item: PoiLibraryItem): PoiLibraryItemDto = PoiLibraryItemDto(
            id = item.id,
            name = item.name,
            latitude = item.latitude,
            longitude = item.longitude,
            elevation = item.elevation,
            category = item.category,
            regionId = item.regionId,
            regionName = item.regionName,
            subCategory = item.subCategory,
            description = item.description,
            aiReason = item.aiReason,
            sourceRouteId = item.sourceRouteId,
            status = item.status,
            createdAt = item.createdAt.toEpochMilli()
        )
    }
}
