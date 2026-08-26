package org.example.route.service

import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskSubmitResponse
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * KML 分析编排服务
 *
 * 职责：
 * 1. 若请求不携带 route_id，委托 RouteBindingService 创建 Route（状态: 3=分析中）
 * 2. 将 route_id 注入 agent 调用请求
 * 3. 调用 KmlAnalysisClientService 提交分析任务
 * 4. 任务提交成功后向 SseTaskEventBus 发布 processing 事件
 *
 * 注意：路线绑定逻辑（含 @Transactional）拆到独立的 RouteBindingService，
 * 避免 Mono.fromCallable 内部 this 调用绕过 Spring AOP 代理导致事务失效。
 */
@Service
class RouteAnalysisOrchestrationService(
    private val routeBindingService: RouteBindingService,
    private val kmlAnalysisClientService: KmlAnalysisClientService,
    private val sseTaskEventBus: SseTaskEventBus
) {
    private val logger = LoggerFactory.getLogger(RouteAnalysisOrchestrationService::class.java)

    /**
     * 提交 KML 分析任务（含路线绑定逻辑）
     *
     * 若请求中不携带 route_id：
     *   - 先在 DB 创建一条 Route 记录（状态: 分析中）
     *   - 将新建的 route_id 注入请求
     * 若携带 route_id：
     *   - 校验路线存在
     *   - 将路线状态更新为分析中
     *
     * agent 任务提交成功后，向 SSE 总线发布 { status: processing, progress: 10 } 事件，
     * 以便已建立 SSE 连接的 Admin 前端收到初始进度推送。
     */
    fun submitAnalysisWithRouteBinding(request: KmlAnalysisSubmitRequest): Mono<TaskSubmitResponse> {
        // 在 boundedElastic 线程池中执行同步 JPA 操作
        // 通过注入的 routeBindingService 调用，确保 @Transactional 代理生效
        return Mono.fromCallable {
            routeBindingService.resolveRouteId(request)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { resolvedRouteId ->
                val enrichedRequest = request.copy(routeId = resolvedRouteId)
                logger.info("提交 KML 分析，routeId=$resolvedRouteId, kmlSource=${request.kmlSource}")
                kmlAnalysisClientService.submitAnalysis(enrichedRequest)
            }
            .doOnSuccess { response ->
                // agent 任务提交成功后先登记任务，再发布 processing 事件。
                val taskId = response.taskId
                logger.info("KML 分析任务提交成功，注册任务并向 SSE 总线发布 processing 事件，taskId=$taskId")
                sseTaskEventBus.registerTask(taskId)
                sseTaskEventBus.publish(
                    taskId,
                    SseProgressEvent(
                        taskId = taskId,
                        status = "processing",
                        progress = 10,
                        currentStep = "分析任务已提交，正在分析中"
                    )
                )
            }
    }
}
