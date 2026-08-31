package org.example.route.service

/**
 * POI 库地区归一化工具
 *
 * 位置合并策略已全部交给 AI 判定（kml-agent-service /api/v1/pois/resolve），
 * 这里只保留地区键归一化：同地区条目才作为候选送给 AI，跨地区不互配。
 */
object PoiLibraryMatching {

    /**
     * 地区归一化键：优先 regionId，其次 regionName；无地区为 null（无地区不与有地区条目互配）
     */
    fun regionKey(regionId: String?, regionName: String?): String? {
        return when {
            !regionId.isNullOrBlank() -> "id:$regionId"
            !regionName.isNullOrBlank() -> "name:${regionName.trim()}"
            else -> null
        }
    }
}
