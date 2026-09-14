package org.example.route.service

import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskSubmitResponse
import org.example.route.repository.RouteMapDataRepository
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
 * 3. 分析输入源解析：请求未携带 KML 文件时，回退使用该路线已存的位置数据
 *    （① RouteMapData.kml_url：绝对 URL 直接透传，相对路径读取本机落盘文件作为 kml_content；
 *    ② 原始文件缺失或不可读时，由 RouteTrackKmlFactory 用有效主轨迹点合成 KML 内容），
 *    两者皆无则拒绝提交
 * 4. 调用 KmlAnalysisClientService 提交分析任务
 * 5. 任务提交成功后向 SseTaskEventBus 发布 processing 事件
 *
 * 注意：路线绑定逻辑（含 @Transactional）拆到独立的 RouteBindingService，
 * 避免 Mono.fromCallable 内部 this 调用绕过 Spring AOP 代理导致事务失效。
 */
@Service
class RouteAnalysisOrchestrationService(
    private val routeBindingService: RouteBindingService,
    private val kmlAnalysisClientService: KmlAnalysisClientService,
    private val routeMapDataRepository: RouteMapDataRepository,
    private val kmlStorageService: KmlStorageService,
    private val sseTaskEventBus: SseTaskEventBus,
    private val routeTrackKmlFactory: RouteTrackKmlFactory
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
     *   - 若未携带 KML 文件（重新分析场景），从该路线已存的 RouteMapData 解析分析输入：
     *     kml_url 为绝对 URL 时直接作为 kml_source；为相对路径时读取本机落盘文件作为 kml_content；
     *     路线没有已存位置数据时拒绝提交
     *
     * agent 任务提交成功后，向 SSE 总线发布 { status: processing, progress: 10 } 事件，
     * 以便已建立 SSE 连接的 Admin 前端收到初始进度推送。
     */
    fun submitAnalysisWithRouteBinding(request: KmlAnalysisSubmitRequest): Mono<TaskSubmitResponse> {
        // 在 boundedElastic 线程池中执行同步 JPA 操作
        // 通过注入的 routeBindingService 调用，确保 @Transactional 代理生效
        return Mono.fromCallable {
            val resolvedRouteId = routeBindingService.resolveRouteId(request)
            resolvedRouteId to resolveAnalysisInput(request, resolvedRouteId)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { (resolvedRouteId, enrichedRequest) ->
                logger.info("提交 KML 分析，routeId=$resolvedRouteId, kmlSource=${enrichedRequest.kmlSource}")
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

    /**
     * 解析分析输入源：
     * - 请求已携带 kml_content 或 kml_source：原样使用
     * - 均未携带且请求绑定已有路线：优先从该路线已存的 RouteMapData.kml_url 回退
     *   （绝对 URL → kml_source；相对路径 → 读取本机落盘文件 → kml_content）；
     *   原始文件缺失或不可读时，回退用有效主轨迹点合成 KML 内容（RouteTrackKmlFactory）
     * - 两种来源都不可用：拒绝（IllegalArgumentException，由调用方转为错误响应）
     */
    private fun resolveAnalysisInput(request: KmlAnalysisSubmitRequest, routeId: String): KmlAnalysisSubmitRequest {
        if (!request.kmlContent.isNullOrBlank() || !request.kmlSource.isNullOrBlank()) {
            return request.copy(routeId = routeId)
        }

        val mapData = routeMapDataRepository.findById(routeId).orElse(null)
        val kmlUrl = mapData?.kmlUrl?.trim()
        val storedContent = when {
            kmlUrl.isNullOrEmpty() -> null
            kmlUrl.startsWith("http://") || kmlUrl.startsWith("https://") -> return request.copy(
                routeId = routeId,
                kmlSource = kmlUrl
            ).also { logger.info("重新分析使用已存 KML URL: routeId=$routeId, kmlUrl=$kmlUrl") }
            else -> kmlStorageService.readStoredContent(kmlUrl)
        }
        if (storedContent != null) {
            logger.info("重新分析使用已存 KML 文件内容: routeId=$routeId, kmlUrl=$kmlUrl")
            return request.copy(routeId = routeId, kmlSource = kmlUrl, kmlContent = storedContent)
        }

        val synthesizedKml = routeTrackKmlFactory.synthesizeCurrentTrackKml(routeId)
        if (synthesizedKml != null) {
            logger.info("原始 KML 文件不可用，回退使用已存轨迹点合成 KML: routeId=$routeId, kmlUrl=$kmlUrl")
            // Agent 合同要求 kml_source 必填；此处携带标识来源的伪 URL 供结果元数据追溯
            return request.copy(routeId = routeId, kmlSource = "db-track://$routeId", kmlContent = synthesizedKml)
        }

        throw IllegalArgumentException(
            "路线 $routeId 没有可用的位置数据（KML 文件或有效轨迹点），无法重新分析；请上传 KML 文件后重试"
        )
    }
}
