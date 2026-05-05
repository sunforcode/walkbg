package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.dto.KmlAnalysisCallbackRequest
import org.example.route.service.KmlAnalysisCallbackService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * KML 分析回调控制器
 * 
 * 用于接收 KML Agent Service 的分析完成回调
 * 
 * API 端点：
 * - POST /api/v1/route-analysis/callback: 接收分析结果
 */
@RestController
@RequestMapping("/api/v1/route-analysis")
@Tag(name = "KML分析回调", description = "KML Agent Service 分析完成后的回调接口")
class KmlAnalysisCallbackController(
    private val callbackService: KmlAnalysisCallbackService
) {
    private val logger = LoggerFactory.getLogger(KmlAnalysisCallbackController::class.java)

    /**
     * 接收 KML 分析完成后的回调
     * 
     * 请求示例：
     * {
     *   "route_id": "route_123",
     *   "task_id": "task_abc123",
     *   "segments": [
     *     {
     *       "id": "seg_001",
     *       "name": "第1段-爬升段(+300m)",
     *       "sequence_number": 1,
     *       "color": "#FF5722",
     *       "distance": 5.2,
     *       "elevation_gain": 300.0,
     *       "elevation_loss": 50.0,
     *       "estimated_time": 45,
     *       "difficulty": 2,
     *       "track_start_index": 0,
     *       "track_end_index": 150,
     *       "start_point": {
     *         "latitude": 39.0123,
     *         "longitude": 113.4567,
     *         "elevation": 2500.0
     *       },
     *       "end_point": {
     *         "latitude": 39.0456,
     *         "longitude": 113.7890,
     *         "elevation": 2800.0
     *       },
     *       "segment_type": "climb",
     *       "slope_direction": "climb",
     *       "avg_slope_degrees": 5.2,
     *       "max_slope_degrees": 12.5,
     *       "confidence": 0.85
     *     }
     *   ]
     * }
     */
    @PostMapping("/callback")
    @Operation(summary = "KML分析回调", description = "KML Agent Service 分析完成后调用此接口")
    fun handleCallback(@RequestBody request: KmlAnalysisCallbackRequest): ResponseEntity<ApiResponse<Any>> {
        logger.info("收到 KML 分析回调，routeId: ${request.routeId}, taskId: ${request.taskId}")
        
        return try {
            callbackService.handleCallback(request)
            logger.info(
                "KML 分析回调处理成功，routeId: ${request.routeId}, " +
                "segments: ${request.segments.size}, " +
                "waterSources: ${request.waterSources.size}, " +
                "campsites: ${request.campsites.size}, " +
                "supplies: ${request.supplies.size}, " +
                "markerPoints: ${request.markerPoints.size}"
            )
            ResponseUtil.success(
                mapOf(
                    "success" to true,
                    "route_id" to request.routeId,
                    "task_id" to request.taskId,
                    "segments_saved" to request.segments.size,
                    "water_sources_saved" to request.waterSources.size,
                    "campsites_saved" to request.campsites.size,
                    "supplies_saved" to request.supplies.size,
                    "marker_points_saved" to request.markerPoints.size,
                    "message" to "分析结果已保存"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("KML 分析回调参数错误: ${e.message}")
            ResponseUtil.badRequest(e.message ?: "参数错误")
        } catch (e: Exception) {
            logger.error("KML 分析回调处理失败", e)
            ResponseUtil.error("处理回调失败: ${e.message}")
        }
    }

    /**
     * 健康检查端点
     * 用于 KML Agent Service 检测回调端点是否可用
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "回调端点健康检查")
    fun healthCheck(): ResponseEntity<ApiResponse<Any>> {
        return ResponseUtil.success(
            mapOf(
                "status" to "healthy",
                "service" to "KmlAnalysisCallbackController",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
