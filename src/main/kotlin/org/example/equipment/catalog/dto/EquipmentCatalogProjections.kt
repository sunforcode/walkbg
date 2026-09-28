package org.example.equipment.catalog.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CatalogBrandProjection(val identity: String, val name: String)
data class CatalogBrandCollection(val items: List<CatalogBrandProjection>)
data class CatalogWeightProjection(@field:Schema(description = "有限正克重", minimum = "0", exclusiveMinimum = true) val grams: Double)

// These are read projections, not persistent or shared specification entities.
@Schema(oneOf = [SimpleSpecificationsProjection::class, BackpackSpecificationsProjection::class,
    TentSpecificationsProjection::class, SleepingBagSpecificationsProjection::class, SleepingPadSpecificationsProjection::class])
sealed interface CatalogDetailSpecifications

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimpleSpecificationsProjection(
    val unitLabel: String,
    val variant: String? = null,
    val unitWeight: CatalogWeightProjection? = null,
    val weightScope: String? = null
) : CatalogDetailSpecifications

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BackpackSpecificationsProjection(
    val unitLabel: String,
    val variant: String? = null,
    val unitWeight: CatalogWeightProjection? = null,
    val weightScope: String? = null,
    @field:Schema(description = "未扩容标称容量，升", minimum = "0", exclusiveMinimum = true)
    val capacityLiters: Double? = null
) : CatalogDetailSpecifications

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TentSpecificationsProjection(
    val unitLabel: String,
    val variant: String? = null,
    val unitWeight: CatalogWeightProjection? = null,
    val weightScope: String? = null,
    @field:Schema(description = "厂家标称容纳人数", minimum = "1") val capacityPersons: Int? = null
) : CatalogDetailSpecifications

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SleepingBagSpecificationsProjection(
    val unitLabel: String,
    val variant: String? = null,
    val unitWeight: CatalogWeightProjection? = null,
    val weightScope: String? = null,
    @field:Schema(description = "舒适温标，摄氏度；允许零和负数") val comfortTemperatureCelsius: Double? = null,
    @field:Schema(description = "下限温标，摄氏度；不替代舒适温标") val limitTemperatureCelsius: Double? = null
) : CatalogDetailSpecifications

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SleepingPadSpecificationsProjection(
    val unitLabel: String,
    val variant: String? = null,
    val unitWeight: CatalogWeightProjection? = null,
    val weightScope: String? = null,
    @field:Schema(allowableValues = ["closed_cell_foam", "air", "self_inflating"]) val padType: String? = null,
    @get:JsonProperty("rValue") @field:Schema(minimum = "0") val rValue: Double? = null,
    @field:Schema(description = "展开使用长度，厘米", minimum = "0", exclusiveMinimum = true) val lengthCm: Double? = null,
    @field:Schema(description = "展开使用时最宽处，厘米", minimum = "0", exclusiveMinimum = true) val maxWidthCm: Double? = null,
    @field:Schema(description = "标称使用厚度，厘米", minimum = "0", exclusiveMinimum = true) val thicknessCm: Double? = null
) : CatalogDetailSpecifications

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CatalogEquipmentSummaryProjection(
    val identity: String,
    val name: String,
    val category: String,
    val equipmentType: String,
    val brand: CatalogBrandProjection? = null,
    val model: String? = null,
    val specifications: SimpleSpecificationsProjection? = null
)

data class CatalogEquipmentCollection(val items: List<CatalogEquipmentSummaryProjection>)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CatalogEquipmentDetailProjection(
    val identity: String,
    val name: String,
    val category: String,
    val equipmentType: String,
    val brand: CatalogBrandProjection? = null,
    val model: String? = null,
    val specifications: CatalogDetailSpecifications? = null
)
