package org.example.guide.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.IdGenerator
import org.example.common.util.ResponseUtil
import org.example.guide.dto.GuideBasicResponse
import org.example.guide.dto.GuideCreateRequest
import org.example.guide.model.Guide
import org.example.guide.service.GuideService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * 攻略控制器
 */
@RestController
@RequestMapping("/api/v1/guides")
@Tag(name = "攻略管理", description = "攻略相关的API接口")
@Validated
class GuideController(
    private val guideService: GuideService
) {

    /**
     * 获取攻略列表
     */
    @GetMapping
    @Operation(summary = "获取攻略列表", description = "支持按标签过滤和分页")
    fun getGuides(
        @Parameter(description = "标签过滤") @RequestParam(required = false) tag: String?,
        @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "20") limit: Int,
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<GuideBasicResponse>>> {
        val pageable = PageRequest.of(page, size)
        val guides = guideService.getGuides(tag, limit, pageable)
        val response = guides.map { GuideBasicResponse.fromGuide(it) }
        return ResponseUtil.successPage(response)
    }

    /**
     * 获取攻略详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取攻略详情", description = "根据攻略ID获取详细信息")
    fun getGuideById(
        @Parameter(description = "攻略ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = guideService.getGuideById(id)
            ?: throw BusinessException.notFound("攻略不存在")

        // 增加浏览次数
        guideService.incrementViewCount(id)

        return ResponseUtil.success(GuideBasicResponse.fromGuide(guide))
    }

    /**
     * 创建攻略
     */
    @PostMapping
    @Operation(summary = "创建攻略", description = "创建新的攻略")
    fun createGuide(
        @Valid @RequestBody request: GuideCreateRequest
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = Guide(
            id = IdGenerator.generateIdWithPrefix("guide"),
            title = request.title,
            content = request.content,
            authorId = request.authorId,
            coverUrl = request.coverUrl,
            tags = request.tags?.joinToString(","),
            status = request.status
        )

        val createdGuide = guideService.createGuide(guide)
        return ResponseUtil.created(GuideBasicResponse.fromGuide(createdGuide), "创建成功")
    }

    /**
     * 更新攻略
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新攻略", description = "更新指定ID的攻略信息")
    fun updateGuide(
        @Parameter(description = "攻略ID") @PathVariable id: String,
        @Valid @RequestBody request: GuideCreateRequest
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = Guide(
            id = id,
            title = request.title,
            content = request.content,
            authorId = request.authorId,
            coverUrl = request.coverUrl,
            tags = request.tags?.joinToString(","),
            status = request.status
        )

        val updatedGuide = guideService.updateGuide(id, guide)
            ?: throw BusinessException.notFound("攻略不存在")

        return ResponseUtil.success(GuideBasicResponse.fromGuide(updatedGuide), "更新成功")
    }

    /**
     * 删除攻略
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除攻略", description = "删除指定ID的攻略")
    fun deleteGuide(
        @Parameter(description = "攻略ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        val deleted = guideService.deleteGuide(id)
        if (!deleted) {
            throw BusinessException.notFound("攻略不存在")
        }
        return ResponseUtil.noContent("删除成功")
    }

    /**
     * 发布攻略
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布攻略", description = "将草稿状态的攻略发布")
    fun publishGuide(
        @Parameter(description = "攻略ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = guideService.publishGuide(id)
            ?: throw BusinessException.notFound("攻略不存在")
        return ResponseUtil.success(GuideBasicResponse.fromGuide(guide), "发布成功")
    }

    /**
     * 下线攻略
     */
    @PostMapping("/{id}/offline")
    @Operation(summary = "下线攻略", description = "将已发布的攻略下线")
    fun takeGuideOffline(
        @Parameter(description = "攻略ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = guideService.takeGuideOffline(id)
            ?: throw BusinessException.notFound("攻略不存在")
        return ResponseUtil.success(GuideBasicResponse.fromGuide(guide), "下线成功")
    }

    /**
     * 点赞攻略
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "点赞攻略", description = "增加攻略的点赞数")
    fun likeGuide(
        @Parameter(description = "攻略ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = guideService.likeGuide(id)
            ?: throw BusinessException.notFound("攻略不存在")
        return ResponseUtil.success(GuideBasicResponse.fromGuide(guide), "点赞成功")
    }

    /**
     * 取消点赞
     */
    @PostMapping("/{id}/unlike")
    @Operation(summary = "取消点赞", description = "减少攻略的点赞数")
    fun unlikeGuide(
        @Parameter(description = "攻略ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<GuideBasicResponse>> {
        val guide = guideService.unlikeGuide(id)
            ?: throw BusinessException.notFound("攻略不存在")
        return ResponseUtil.success(GuideBasicResponse.fromGuide(guide), "取消点赞成功")
    }
}
