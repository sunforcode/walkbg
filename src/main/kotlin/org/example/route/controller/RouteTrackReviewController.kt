package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.dto.MainTrackReviewRequest
import org.example.route.dto.MainTrackReviewResponse
import org.example.route.service.RouteTrackReviewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/routes/{id}/main-track-review")
@Tag(name = "路线管理（主轨迹审核）")
class RouteTrackReviewController(private val reviews: RouteTrackReviewService) {
    @GetMapping
    @Operation(summary = "查看当前候选主轨迹与审核状态")
    fun read(@PathVariable id: String): ResponseEntity<ApiResponse<MainTrackReviewResponse>> =
        ResponseUtil.success(reviews.read(id))

    @PostMapping
    @Operation(summary = "保存候选主轨迹审核结论", description = "绑定当前候选和审核版本；不发布或修改公开版本")
    fun submit(@PathVariable id: String, @RequestBody request: MainTrackReviewRequest): ResponseEntity<ApiResponse<MainTrackReviewResponse>> =
        ResponseUtil.success(reviews.submit(id, request), "审核已保存，需发布或重新发布后公开生效")
}
