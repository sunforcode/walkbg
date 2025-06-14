package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import org.example.route.dto.DailyPlanCreateRequest

/**
 * 路线创建请求DTO
 */
data class RouteCreateRequest(
    val id: String? = null,

    @field:NotBlank(message = "路线名称不能为空")
    @field:Size(max = 200, message = "路线名称长度不能超过200个字符")
    val name: String,

    @field:Size(max = 2000, message = "路线描述长度不能超过2000个字符")
    val description: String?,

    @field:Size(max = 100, message = "区域名称长度不能超过100个字符")
    val region: String?,

    @JsonProperty("region_id")
    @field:Size(max = 64, message = "区域ID长度不能超过64个字符")
    val regionId: String?,

    @field:DecimalMin(value = "0.0", message = "距离不能为负数")
    @field:DecimalMax(value = "10000.0", message = "距离不能超过10000公里")
    val distance: BigDecimal?,

    @field:Min(value = 0, message = "预计时长不能为负数")
    @field:Max(value = 8760, message = "预计时长不能超过8760小时")
    val duration: Int?,

    @field:DecimalMin(value = "-90.0", message = "纬度范围应在-90到90之间")
    @field:DecimalMax(value = "90.0", message = "纬度范围应在-90到90之间")
    val latitude: BigDecimal?,

    @field:DecimalMin(value = "-180.0", message = "经度范围应在-180到180之间")
    @field:DecimalMax(value = "180.0", message = "经度范围应在-180到180之间")
    val longitude: BigDecimal?,

    @field:DecimalMin(value = "-500.0", message = "海拔不能低于-500米")
    @field:DecimalMax(value = "10000.0", message = "海拔不能超过10000米")
    val altitude: BigDecimal?,

    @JsonProperty("elevation_gain")
    @field:DecimalMin(value = "0.0", message = "爬升不能为负数")
    @field:DecimalMax(value = "10000.0", message = "爬升不能超过10000米")
    val elevationGain: BigDecimal?,

    @JsonProperty("elevation_loss")
    @field:DecimalMin(value = "0.0", message = "下降不能为负数")
    @field:DecimalMax(value = "10000.0", message = "下降不能超过10000米")
    val elevationLoss: BigDecimal?,

    @field:Min(value = 1, message = "难度等级范围应在1-5之间")
    @field:Max(value = 5, message = "难度等级范围应在1-5之间")
    val difficulty: Int?,

    @JsonProperty("route_type")
    @field:Min(value = 0, message = "路线类型范围应在0-3之间")
    @field:Max(value = 3, message = "路线类型范围应在0-3之间")
    val routeType: Int?,

    @JsonProperty("route_direction")
    @field:Min(value = 0, message = "路线方向值无效")
    @field:Max(value = 360, message = "路线方向值无效")
    val routeDirection: Int?,

    @field:Min(value = 0, message = "状态值范围应在0-2之间")
    @field:Max(value = 2, message = "状态值范围应在0-2之间")
    val status: Int = 1,

    @JsonProperty("cover_url")
    @field:Size(max = 500, message = "封面图片URL长度不能超过500个字符")
    @field:Pattern(regexp = "^(https?://.*|)$", message = "封面图片URL格式不正确")
    val coverUrl: String?,

    @JsonProperty("map_data_id")
    @field:Size(max = 64, message = "地图数据ID长度不能超过64个字符")
    val mapDataId: String?,

    @JsonProperty("default_map_id")
    @field:Size(max = 64, message = "默认地图ID长度不能超过64个字符")
    val defaultMapId: String = "",

    @JsonProperty("created_by")
    @field:NotBlank(message = "创建者ID不能为空")
    @field:Size(max = 64, message = "创建者ID长度不能超过64个字符")
    val createdBy: String,
    
    // 关联数据
    @field:Size(max = 20, message = "标签数量不能超过20个")
    val tags: List<@Size(max = 50, message = "标签长度不能超过50个字符") String> = emptyList(),

    @field:Size(max = 4, message = "季节数量不能超过4个")
    val seasons: List<@Size(max = 20, message = "季节名称长度不能超过20个字符") String> = emptyList(),

    @field:Valid
    @field:Size(max = 100, message = "路点数量不能超过100个")
    val waypoints: List<WaypointCreateRequest> = emptyList(),

    @field:Valid
    @field:Size(max = 50, message = "路段数量不能超过50个")
    val segments: List<SegmentCreateRequest> = emptyList(),

    @field:Valid
    @field:Size(max = 20, message = "图片数量不能超过20张")
    val images: List<RouteImageCreateRequest> = emptyList(),

    // 补给点
    @field:Valid
    @field:Size(max = 50, message = "补给点数量不能超过50个")
    val supplies: List<org.example.route.dto.SupplyCreateRequest> = emptyList(),

    // 营地
    @field:Valid
    @field:Size(max = 30, message = "营地数量不能超过30个")
    val campsites: List<CampsiteCreateRequest> = emptyList(),

    // 标记点
    @field:Valid
    @field:Size(max = 100, message = "标记点数量不能超过100个")
    val markerPoints: List<MarkerPointCreateRequest> = emptyList(),

    // 日程计划
    @field:Valid
    @field:Size(max = 30, message = "日程计划数量不能超过30个")
    val dailyPlans: List<DailyPlanCreateRequest> = emptyList()
)



/**
 * 扩展方法：将RouteCreateRequest转换为Route实体
 */
fun RouteCreateRequest.toRoute(): org.example.route.model.Route {
    return org.example.route.model.Route(
        id = this.id ?: java.util.UUID.randomUUID().toString(),
        name = this.name,
        description = this.description,
        region = this.region,
        regionId = this.regionId,
        difficulty = this.difficulty,
        routeType = this.routeType,
        status = this.status,
        coverUrl = this.coverUrl,
        defaultMapId = this.defaultMapId?.takeIf { it.isNotBlank() } ?: "",
        createdBy = this.createdBy
    )
}
