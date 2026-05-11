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
     *   "segment_schemes": [
     *     {
     *       "scheme_type": "slope",
     *       "label": "按坡度",
     *       "is_default": true,
     *       "segments": [...]
     *     }
     *   ],
     *   "poi_points": [
     *     {
     *       "name": "垭口",
     *       "latitude": 39.0123,
     *       "longitude": 113.4567,
     *       "elevation": 3200.0,
     *       "category": "pass",
     *       "source": "kml_marker",
     *       "confidence": 1.0
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
                "segmentSchemes: ${request.segmentSchemes.size}, " +
                "poiPoints: ${request.poiPoints.size}"
            )
            ResponseUtil.success(
                mapOf(
                    "success" to true,
                    "route_id" to request.routeId,
                    "task_id" to request.taskId,
                    "segment_schemes_saved" to request.segmentSchemes.size,
                    "poi_points_saved" to request.poiPoints.size,
                    "message" to "分析结果已保存"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("KML 分析回调参数错误: ${e.message}")
            ResponseUtil.error(e.message ?: "参数错误", 400)
        } catch (e: Exception) {
            logger.error("KML 分析回调处理失败", e)
            ResponseUtil.error("处理回调失败: ${e.message}")
        }
    }

    /**
     * 回调端点健康检查
     * 用于 KML Agent Service 检测回调端点是否可用
     */
    @GetMapping("/callback/health")
    @Operation(summary = "回调端点健康检查", description = "KML Agent Service 检测回调端点是否可用")
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
