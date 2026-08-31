package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.dto.PoiFilterPreviewRequest
import org.example.route.dto.PoiFilterPreviewResponse
import org.example.route.dto.PoiLibraryItemDto
import org.example.route.dto.PoiLibrarySaveRequest
import org.example.route.dto.PoiLibrarySaveResponse
import org.example.route.service.PoiLibraryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 全局 POI 库控制器
 *
 * - POST /filter-preview: AI（LLM）筛选路线 POI，返回预览（保留/剔除+理由），不落库
 * - POST /save: 人工确认后入库，并回写路线 POI 为已采纳
 * - GET /: 查询库内全部条目
 */
@RestController
@RequestMapping("/api/v1/poi-library")
@Tag(name = "POI 库", description = "全局 POI 库：AI 筛选、确认入库、复用")
class PoiLibraryController(
    private val poiLibraryService: PoiLibraryService
) {

    @PostMapping("/filter-preview")
    @Operation(summary = "AI 筛选预览", description = "用 LLM 筛选路线 POI，返回保留/剔除建议与理由，不修改数据")
    fun filterPreview(@RequestBody request: PoiFilterPreviewRequest): ResponseEntity<ApiResponse<PoiFilterPreviewResponse>> {
        return try {
            ResponseUtil.success(poiLibraryService.filterPreview(request.routeId), "AI 筛选完成")
        } catch (e: Exception) {
            ResponseUtil.error("AI 筛选失败: ${e.message}")
        }
    }

    @PostMapping("/save")
    @Operation(summary = "确认入库", description = "把人工确认的 POI 存入全局库（按名称+距离去重），并回写路线 POI 为已采纳")
    fun save(@RequestBody request: PoiLibrarySaveRequest): ResponseEntity<ApiResponse<PoiLibrarySaveResponse>> {
        return try {
            ResponseUtil.success(poiLibraryService.save(request), "POI 已入库")
        } catch (e: Exception) {
            ResponseUtil.error("POI 入库失败: ${e.message}")
        }
    }

    @GetMapping
    @Operation(summary = "查询库内条目", description = "返回全局 POI 库全部有效条目")
    fun list(): ResponseEntity<ApiResponse<List<PoiLibraryItemDto>>> {
        return try {
            ResponseUtil.success(poiLibraryService.list().map { PoiLibraryItemDto.fromPoiLibraryItem(it) }, "获取 POI 库成功")
        } catch (e: Exception) {
            ResponseUtil.error("获取 POI 库失败: ${e.message}")
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移除库内条目", description = "从全局 POI 库中移除指定条目（软删除）")
    fun remove(@PathVariable id: String): ResponseEntity<ApiResponse<Boolean>> {
        return try {
            val removed = poiLibraryService.remove(id)
            if (removed) {
                ResponseUtil.success(true, "POI 已从库中移除")
            } else {
                ResponseUtil.error("未找到指定的 POI 库条目")
            }
        } catch (e: Exception) {
            ResponseUtil.error("移除 POI 失败: ${e.message}")
        }
    }
}
