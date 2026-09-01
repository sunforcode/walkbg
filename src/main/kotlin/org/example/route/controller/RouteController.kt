package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.exception.BusinessException
import org.example.common.util.ResponseUtil
import org.example.route.dto.PoiPointDto
import org.example.route.dto.RouteBasicResponse
import org.example.route.dto.RouteDetailResponse
import org.example.route.dto.SegmentDto
import org.example.route.dto.SegmentSchemeDto
import org.example.route.repository.PoiPointRepository
import org.example.route.repository.SegmentRepository
import org.example.route.repository.SegmentSchemeRepository
import org.example.route.service.RouteApplicationService
import org.example.route.service.SegmentEditService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

/**
 * 拆分路段请求
 */
data class SplitSegmentRequest(
    @com.fasterxml.jackson.annotation.JsonProperty("split_track_index")
    val splitTrackIndex: Int,
    val splitPoint: org.example.route.service.SplitPointRequest? = null
)

/**
 * 合并路段请求
 */
data class MergeSegmentsRequest(
    @com.fasterxml.jackson.annotation.JsonProperty("segment_ids")
    val segmentIds: List<String>,
    val name: String? = null
)

/**
 * 改名请求
 */
data class RenameRequest(
    val name: String
)

/**
 * 路线控制器
 * 只处理HTTP请求响应，业务逻辑委托给ApplicationService
 */
@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "路线管理", description = "路线相关的API接口")
@Validated
class RouteController(
    private val routeApplicationService: RouteApplicationService,
    private val routeService: org.example.route.service.RouteService,
    private val userRouteFavoriteRepository: org.example.route.repository.UserRouteFavoriteRepository,
    private val userRouteCompletionRepository: org.example.route.repository.UserRouteCompletionRepository,
    private val routeRepository: org.example.route.repository.RouteRepository,
    private val segmentSchemeRepository: SegmentSchemeRepository,
    private val segmentRepository: SegmentRepository,
    private val poiPointRepository: PoiPointRepository,
    private val segmentEditService: SegmentEditService
) {

    /**
     * 分页查询路线列表（支持统一参数）
     * 
     * 支持抽象参数：
     * - category: 路线类别（hiking/cycling/camping/climbing/urban/mountain/coastal 或中文：徒步/骑行/露营/攀岩/城市/山地/海滨）
     * - tags: 标签（逗号分隔，如："春季,赏花"）
     * - difficulty: 难度（支持数字 1-5 或字符串 easy/medium/hard）
     * - routeType: 路线类型（支持数字 0-3 或字符串 roundtrip/loop/oneway/multiday）
     * - sort: 排序方式（popular/new/distance 或 热门/最新/距离）
     */
    @GetMapping
    @Operation(summary = "分页查询路线列表", description = "获取路线列表，支持分页和统一参数过滤")
    fun getRoutes(
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "关键词搜索") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "路线类别（hiking/cycling/camping/climbing/urban/mountain/coastal 或中文）") @RequestParam(required = false) category: String?,
        @Parameter(description = "标签（逗号分隔，如：春季,赏花）") @RequestParam(required = false) tags: String?,
        @Parameter(description = "区域ID") @RequestParam(required = false) regionId: String?,
        @Parameter(description = "难度等级（支持数字 1-5 或字符串 easy/medium/hard）") @RequestParam(required = false) difficulty: String?,
        @Parameter(description = "路线类型（支持数字 0-3 或字符串 roundtrip/loop/oneway/multiday）") @RequestParam(required = false) routeType: String?,
        @Parameter(description = "最小距离") @RequestParam(required = false) minDistance: Double?,
        @Parameter(description = "最大距离") @RequestParam(required = false) maxDistance: Double?,
        @Parameter(description = "用户ID（查询收藏路线时使用）") @RequestParam(required = false) userId: String?,
        @Parameter(description = "排序方式（popular/new/distance 或 热门/最新/距离）") @RequestParam(defaultValue = "popular") sort: String?
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val pageable = org.springframework.data.domain.PageRequest.of(
            page, size,
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        )
        // Admin 后台展示所有状态的路线（不过滤 status），使用 searchRoutes 而非 searchRoutesUnified
        val routes = routeApplicationService.searchRoutes(
            keyword = keyword,
            difficulty = difficulty?.toIntOrNull(),
            pageable = pageable
        )
        return ResponseUtil.successPage(routes)
    }

    /**
     * 根据ID查询路线详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询路线详情", description = "根据路线ID获取详细信息")
    fun getRouteById(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam(required = false) userId: String?
    ): ResponseEntity<ApiResponse<RouteDetailResponse>> {
        val route = routeApplicationService.getRouteFullDetails(id, userId)
            ?: throw BusinessException.notFound("路线不存在")
        return ResponseUtil.success(route)
    }

    /**
     * 创建路线
     */
    @PostMapping
    @Operation(summary = "创建路线", description = "创建新的路线")
    fun createRoute(
        @RequestBody @Valid request: org.example.route.dto.RouteCreateRequest
    ): ResponseEntity<ApiResponse<RouteBasicResponse>> {
        val route = routeApplicationService.createCompleteRoute(request)
        return ResponseUtil.created(route, "路线创建成功")
    }

    /**
     * 更新路线基本信息（管理端）
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新路线基本信息", description = "仅更新请求中出现的字段；分析中的路线不可编辑")
    fun updateRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @RequestBody @Valid request: org.example.route.dto.RouteUpdateRequest
    ): ResponseEntity<ApiResponse<RouteDetailResponse>> {
        return ResponseUtil.success(routeApplicationService.updateRouteBasic(id, request), "路线已更新")
    }

    /**
     * 路线状态流转（管理端）
     */
    @PostMapping("/{id}/status")
    @Operation(
        summary = "路线状态流转",
        description = "合法迁移：0→1 发布（含发布前检查）、1→0 下线、1→2 关闭、2→0 重新开启、2→1 重新发布；分析中(3)不可手动变更"
    )
    fun changeRouteStatus(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @RequestBody @Valid request: org.example.route.dto.RouteStatusUpdateRequest
    ): ResponseEntity<ApiResponse<RouteDetailResponse>> {
        return ResponseUtil.success(
            routeApplicationService.changeRouteStatus(id, request.targetStatus, request.reason),
            "路线状态已更新"
        )
    }

    /**
     * 删除路线（管理端，软删除）
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除路线（软删除）",
        description = "分析中的路线不可删除；被未取消行程引用时默认拒绝，force=true 强制删除"
    )
    fun deleteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "强制删除（忽略行程引用检查）") @RequestParam("force", required = false, defaultValue = "false") force: Boolean
    ): ResponseEntity<ApiResponse<Nothing>> {
        routeApplicationService.deleteRoute(id, force)
        return ResponseUtil.success(null, "路线已删除")
    }

    /**
     * 合并多个路段为一个
     */
    @PostMapping("/{id}/segments/merge")
    @Operation(summary = "合并路段", description = "将选中的多个路段（同一方案）合并为一个，轨迹区间取并集，数值累加")
    fun mergeSegments(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @RequestBody request: MergeSegmentsRequest
    ): ResponseEntity<ApiResponse<SegmentDto>> {
        return ResponseUtil.success(
            segmentEditService.mergeSegments(id, request.segmentIds, request.name),
            "路段合并成功"
        )
    }

    /**
     * 路段改名
     */
    @PutMapping("/{id}/segments/{segmentId}/name")
    @Operation(summary = "路段改名", description = "修改路段名称")
    fun renameSegment(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "路段ID") @PathVariable segmentId: String,
        @RequestBody request: RenameRequest
    ): ResponseEntity<ApiResponse<SegmentDto>> {
        return ResponseUtil.success(
            segmentEditService.renameSegment(id, segmentId, request.name),
            "路段已改名"
        )
    }

    /**
     * POI 改名
     */
    @PutMapping("/{id}/pois/{poiId}/name")
    @Operation(summary = "POI 改名", description = "修改 POI 名称")
    fun renamePoi(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "POI ID") @PathVariable poiId: String,
        @RequestBody request: RenameRequest
    ): ResponseEntity<ApiResponse<PoiPointDto>> {
        return ResponseUtil.success(
            segmentEditService.renamePoi(id, poiId, request.name),
            "POI 已改名"
        )
    }

    // =========================================================================
    // 路段/POI 人工编辑（拆分/采纳）
    // =========================================================================

    /**
     * 拆分路段：在指定轨迹索引处把一段拆为两段
     */
    @PostMapping("/{id}/segments/{segmentId}/split")
    @Operation(summary = "拆分路段", description = "在指定轨迹索引处把路段拆为两段")
    fun splitSegment(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "路段ID") @PathVariable segmentId: String,
        @RequestBody request: SplitSegmentRequest
    ): ResponseEntity<ApiResponse<Map<String, SegmentDto>>> {
        val (segA, segB) = segmentEditService.splitSegment(id, segmentId, request.splitTrackIndex, request.splitPoint)
        return ResponseUtil.success(mapOf("front" to segA, "back" to segB), "路段拆分成功")
    }

    /**
     * 采纳单个草稿路段
     */
    @PostMapping("/{id}/segments/{segmentId}/adopt")
    @Operation(summary = "采纳草稿路段", description = "将分析建议的草稿路段确认为正式数据")
    fun adoptSegment(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "路段ID") @PathVariable segmentId: String
    ): ResponseEntity<ApiResponse<SegmentDto>> {
        return ResponseUtil.success(segmentEditService.adoptSegment(id, segmentId), "路段已采纳")
    }

    /**
     * 采纳路线全部草稿路段
     */
    @PostMapping("/{id}/segments/adopt-all")
    @Operation(summary = "批量采纳草稿路段", description = "将路线下所有草稿路段确认为正式数据")
    fun adoptAllSegments(
        @Parameter(description = "路线ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val count = segmentEditService.adoptAllSegments(id)
        return ResponseUtil.success(mapOf("adopted" to count), "已采纳 $count 个路段")
    }

    /**
     * 采纳单个草稿 POI
     */
    @PostMapping("/{id}/pois/{poiId}/adopt")
    @Operation(summary = "采纳草稿 POI", description = "将分析建议的草稿 POI 确认为正式数据")
    fun adoptPoi(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "POI ID") @PathVariable poiId: String
    ): ResponseEntity<ApiResponse<PoiPointDto>> {
        return ResponseUtil.success(segmentEditService.adoptPoi(id, poiId), "POI 已采纳")
    }

    /**
     * 采纳路线全部草稿 POI
     */
    @PostMapping("/{id}/pois/adopt-all")
    @Operation(summary = "批量采纳草稿 POI", description = "将路线下所有草稿 POI 确认为正式数据")
    fun adoptAllPois(
        @Parameter(description = "路线ID") @PathVariable id: String
    ): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val count = segmentEditService.adoptAllPois(id)
        return ResponseUtil.success(mapOf("adopted" to count), "已采纳 $count 个 POI")
    }


    /**
     * 收藏路线
     */
    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏路线", description = "将指定路线添加到收藏")
    fun favoriteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 验证路线是否存在
        routeRepository.findById(id).orElseThrow {
            BusinessException.notFound("路线不存在")
        }
        
        // 幂等性处理：如果已经收藏，则不重复创建
        if (!userRouteFavoriteRepository.existsByUserIdAndRouteId(userId, id)) {
            val favorite = org.example.route.model.UserRouteFavorite(
                userId = userId,
                routeId = id
            )
            userRouteFavoriteRepository.save(favorite)
        }
        
        return ResponseUtil.success(null, "收藏成功")
    }

    /**
     * 取消收藏路线
     */
    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏路线", description = "从收藏中移除指定路线")
    fun unfavoriteRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 幂等性处理：如果没有收藏，则不报错
        userRouteFavoriteRepository.deleteByUserIdAndRouteId(userId, id)
        return ResponseUtil.success(null, "取消收藏成功")
    }

    /**
     * 完成路线
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成路线", description = "标记路线为已完成")
    fun completeRoute(
        @Parameter(description = "路线ID") @PathVariable id: String,
        @Parameter(description = "用户ID") @RequestParam userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 验证路线是否存在
        val route = routeRepository.findById(id).orElseThrow {
            BusinessException.notFound("路线不存在")
        }
        
        // 幂等性处理：每个用户每次完成路线都会需要记录，但usage_count仅在第一次有效完成时增加
        val existingCompletion = userRouteCompletionRepository.findByUserIdAndRouteId(userId, id)
        if (existingCompletion == null) {
            // 第一次完成，需要更新路线的usage_count
            val completion = org.example.route.model.UserRouteCompletion(
                userId = userId,
                routeId = id
            )
            userRouteCompletionRepository.save(completion)
            
            // 更新路线的usage_count
            route.incrementUsageCount()
            routeRepository.save(route)
        }
        
        return ResponseUtil.success(null, "路线完成记录成功")
    }

    /**
     * 查询我创建的路线
     */
    @GetMapping("/my")
    @Operation(summary = "查询我创建的路线", description = "获取当前用户创建的路线列表")
    fun getMyRoutes(
        @Parameter(description = "用户ID") @RequestParam userId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        val routes = routeRepository.findByCreatedBy(userId, pageable)
        val result = routes.map { routeApplicationService.enrichRouteBasic(it) }
        return ResponseUtil.successPage(result)
    }

    /**
     * 查询我收藏的路线
     */
    @GetMapping("/favorites")
    @Operation(summary = "查询我收藏的路线", description = "获取当前用户收藏的路线列表")
    fun getFavoriteRoutes(
        @Parameter(description = "用户ID") @RequestParam userId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val favorites = userRouteFavoriteRepository.findByUserId(userId, pageable)
        // 关联路线信息
        val result = favorites.map { favorite ->
            routeRepository.findById(favorite.routeId).map { routeApplicationService.enrichRouteBasic(it) }.orElse(null)
        }.filterNotNull()
        // 重新币造Page对象
        return ResponseUtil.successPage(
            org.springframework.data.domain.PageImpl(
                result,
                pageable,
                favorites.totalElements
            )
        )
    }

    /**
     * 查询我完成的路线
     */
    @GetMapping("/completed")
    @Operation(summary = "查询我完成的路线", description = "获取当前用户完成的路线列表")
    fun getCompletedRoutes(
        @Parameter(description = "用户ID") @RequestParam userId: String,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        val completions = userRouteCompletionRepository.findByUserId(userId, pageable)
        // 关联路线信息
        val result = completions.map { completion ->
            routeRepository.findById(completion.routeId).map { routeApplicationService.enrichRouteBasic(it) }.orElse(null)
        }.filterNotNull()
        // 重新造活Page对象
        return ResponseUtil.successPage(
            org.springframework.data.domain.PageImpl(
                result,
                pageable,
                completions.totalElements
            )
        )
    }


    /**
     * 获取热门路线
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门路线", description = "按热度排序返回热门路线列表")
    fun getPopularRoutes(
        @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val routes = routeApplicationService.getPopularRoutes(limit)
        return ResponseUtil.successPage(routes)
    }

    /**
     * 获取新晋路线
     */
    @GetMapping("/new")
    @Operation(summary = "获取新晋路线", description = "按创建时间降序返回新晋路线列表")
    fun getNewRoutes(
        @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val routes = routeApplicationService.getNewRoutes(limit)
        return ResponseUtil.successPage(routes)
    }

    /**
     * 获取季节性路线
     */
    @GetMapping("/seasonal")
    @Operation(summary = "获取季节性路线", description = "根据当前季节或指定季节返回路线列表")
    fun getSeasonalRoutes(
        @Parameter(description = "季节（可选：春季/夏季/秋季/冬季），不指定则根据当前月份自动判断") @RequestParam(required = false) season: String?,
        @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val routes = routeApplicationService.getSeasonalRoutes(season, limit)
        return ResponseUtil.successPage(routes)
    }

    /**
     * 获取周末路线
     */
    @GetMapping("/weekend")
    @Operation(summary = "获取周末路线", description = "返回适合周末出行的短途路线列表")
    fun getWeekendRoutes(
        @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
        val routes = routeApplicationService.getWeekendRoutes(limit)
        return ResponseUtil.successPage(routes)
    }

    // =========================================================================
    // v2: 多方案分段 & POI 端点
    // =========================================================================

    /**
     * 按方案类型查询路线分段方案
     *
     * @param id         路线 ID
     * @param schemeType 方案类型：slope|day|terrain|road_type（不传则返回默认方案）
     */
    @GetMapping("/{id}/segments")
    @Operation(summary = "查询路线分段方案", description = "支持 scheme_type 参数指定方案类型")
    fun getRouteSegments(
        @Parameter(description = "路线 ID") @PathVariable id: String,
        @Parameter(description = "方案类型：slope|day|terrain|road_type") @RequestParam("scheme_type", required = false) schemeType: String?
    ): ResponseEntity<ApiResponse<List<SegmentSchemeDto>>> {
        return try {
            val schemes = if (schemeType != null) {
                val scheme = segmentSchemeRepository.findByRouteIdAndSchemeType(id, schemeType)
                if (scheme != null) listOf(scheme) else emptyList()
            } else {
                val defaultScheme = segmentSchemeRepository.findByRouteIdAndIsDefaultTrue(id)
                if (defaultScheme != null) listOf(defaultScheme)
                else segmentSchemeRepository.findByRouteId(id)
            }
            val result = schemes.map { scheme ->
                val segs = segmentRepository.findByRouteId(id)
                    .filter { it.schemeId == scheme.id }
                    .map { SegmentDto.fromSegment(it) }
                SegmentSchemeDto.fromScheme(scheme, segs)
            }
            ResponseUtil.success(result, "获取分段方案成功")
        } catch (e: Exception) {
            ResponseUtil.error("获取分段方案失败: ${e.message}")
        }
    }

    /**
     * 查询路线的 POI 点
     *
     * @param id       路线 ID
     * @param category 可选：water|camp|supply|photo|pass|valley|weather|danger|start|end
     */
    @GetMapping("/{id}/pois")
    @Operation(summary = "查询路线 POI 点", description = "支持 category 参数筛选，不传则返回全部")
    fun getRoutePois(
        @Parameter(description = "路线 ID") @PathVariable id: String,
        @Parameter(description = "POI 类型: water|camp|supply|photo|pass|valley|weather|danger|start|end") @RequestParam("category", required = false) category: String?
    ): ResponseEntity<ApiResponse<List<PoiPointDto>>> {
        return try {
            val pois = if (category != null) {
                poiPointRepository.findByRouteIdAndCategory(id, category)
            } else {
                poiPointRepository.findByRouteId(id)
            }
            ResponseUtil.success(pois.map { PoiPointDto.fromPoiPoint(it) }, "获取 POI 点成功")
        } catch (e: Exception) {
            ResponseUtil.error("获取 POI 点失败: ${e.message}")
        }
    }
}