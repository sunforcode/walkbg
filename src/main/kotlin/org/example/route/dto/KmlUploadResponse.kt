package org.example.route.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * KML 上传响应DTO
 *
 * kml_url 为相对路径，前端需拼接 origin 得到完整 URL 后
 * 作为 kml_source 传给 POST /api/v1/route-analysis/analyze
 */
data class KmlUploadResponse(
    @JsonProperty("kml_url")
    val kmlUrl: String,

    @JsonProperty("file_size")
    val fileSize: Long
)
