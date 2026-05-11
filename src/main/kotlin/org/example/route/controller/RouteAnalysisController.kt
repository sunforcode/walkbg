package org.example.route.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.common.dto.ApiResponse
import org.example.common.util.ResponseUtil
import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskStatusResponse
import org.example.route.dto.TaskSubmitResponse
import org.example.route.service.KmlAnalysisClientService
import org.example.route.service.RouteAnalysisOrchestrationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/route-analysis")
@Tag(name = "KML分析服务", description = "通过后端代理调用 KML Agent Service 进行路线分析")
class RouteAnalysisController(
    private val kmlAnalysisClientService: KmlAnalysisClientService,
    private val routeAnalysisOrchestrationService: RouteAnalysisOrchestrationService
) {
    private val logger = LoggerFactory.getLogger(RouteAnalysisController::class.java)

    @GetMapping("/health")
    @Operation(summary = "检查 KML Agent Service 健康状态", description = "通过后端代理检查 KML Agent Service 是否可用")
    fun checkAgentHealth(): Mono<ResponseEntity<ApiResponse<Map<String, Any?>>>> {
        return kmlAnalysisClientService.healthCheck()
            .map { health ->
                ResponseUtil.success<Map<String, Any?>>(
                    mapOf(
                        "available" to true,
                        "status" to health.status,
                        "version" to health.version,
                        "checks" to health.checks
                    )
                )
            }
            .onErrorResume { error ->
                logger.warn("KML Agent Service 不可用: ${error.message}")
                Mono.just(
                    ResponseUtil.success<Map<String, Any?>>(
                        mapOf(
                            "available" to false,
                            "status" to "unavailable",
                            "message" to "无法连接到 KML Agent Service: ${error.message}"
                        )
                    )
                )
            }
    }

    /**
     * 提交 KML 分析任务
     *
     * 若请求中包含 route_id，则绑定到已有路线并更新其状态为"分析中"。
     * 若不包含 route_id，则自动创建一条新路线（状态: 分析中），再触发分析。
     * 分析完成后 Agent 通过 callback 将结果落库，路线状态恢复为"规划中"。
     */
    @PostMapping("/analyze")
    @Operation(
        summary = "提交 KML 分析任务",
        description = "通过后端代理提交 KML 分析任务。若不传 route_id，自动创建路线后再触发分析。"
    )
    fun submitAnalysis(
        @RequestBody request: KmlAnalysisSubmitRequest
    ): Mono<ResponseEntity<ApiResponse<TaskSubmitResponse>>> {
        logger.info("收到 KML 分析请求: kmlSource=${request.kmlSource}, routeId=${request.routeId}")

        return routeAnalysisOrchestrationService.submitAnalysisWithRouteBinding(request)
            .map { response ->
                ResponseUtil.success(response, "分析任务提交成功")
            }
            .onErrorResume { error ->
                logger.error("提交分析任务失败: ${error.message}", error)
                Mono.just(
                    ResponseUtil.error("提交分析任务失败: ${error.message}")
                )
            }
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态", description = "通过后端代理查询 KML 分析任务的执行状态")
    fun getTaskStatus(
        @PathVariable taskId: String
    ): Mono<ResponseEntity<ApiResponse<TaskStatusResponse>>> {
        logger.info("查询任务状态: taskId=$taskId")

        return kmlAnalysisClientService.getTaskStatus(taskId)
            .map { response ->
                ResponseUtil.success(response)
            }
            .onErrorResume { error ->
                logger.error("查询任务状态失败: ${error.message}", error)
                Mono.just(
                    ResponseUtil.error("查询任务状态失败: ${error.message}")
                )
            }
    }
}
