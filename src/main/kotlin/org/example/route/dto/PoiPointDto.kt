package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.route.model.PoiPoint

/**
 * 统一附属信息点 DTO（对外 API 返回给 App）
 *
 * card_data 以 Any? 形式透传，App 端按 category 自行解析。
 */
data class PoiPointDto(
    val id: String,
    @JsonProperty("route_id")
    val routeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val category: String,
    @JsonProperty("sub_category")
    val subCategory: String?,
    val source: String,
    val description: String?,
    val confidence: Double?,
    /**
     * 扩展属性，各 category 结构不同，透传 JSON
     */
    @JsonProperty("card_data")
    val cardData: Any? = null
) {
    companion object {
        private val mapper = ObjectMapper()

        fun fromPoiPoint(poi: PoiPoint): PoiPointDto {
            // 将 JSON 字符串反序列化为 Map，方便 JSON 序列化时不双重编码
            val cardData: Any? = poi.cardData?.let {
                try {
                    mapper.readValue(it, Map::class.java)
                } catch (_: Exception) {
                    it  // 反序列化失败则直接透传字符串
                }
            }
            return PoiPointDto(
                id = poi.id,
                routeId = poi.routeId,
                name = poi.name,
                latitude = poi.latitude,
                longitude = poi.longitude,
                elevation = poi.elevation,
                category = poi.category,
                subCategory = poi.subCategory,
                source = poi.source,
                description = poi.description,
                confidence = poi.confidence,
                cardData = cardData
            )
        }
    }
}
