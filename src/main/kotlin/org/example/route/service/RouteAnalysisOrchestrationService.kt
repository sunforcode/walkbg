package org.example.route.service

import org.example.common.exception.BusinessException
import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskStatusResponse
import org.example.route.dto.TaskSubmitResponse
import org.example.route.repository.RouteMapDataRepository
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Input persistence -> committed reservation -> HTTP -> conditional acknowledgement/compensation. */
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

    private enum class SubmissionDelivery { WAITING, DELIVERED, CANCELLED }

    fun submitAnalysisWithRouteBinding(request: KmlAnalysisSubmitRequest): Mono<TaskSubmitResponse> =
        Mono.defer {
            // Each subscription owns its reservation and delivery/cancellation decision.
            val delivery = AtomicReference(SubmissionDelivery.WAITING)
            val reservation = AtomicReference<Pair<String, String>?>(null)
            val cancellationScheduled = AtomicBoolean(false)
            fun compensateCancellation() {
                val (routeId, taskId) = reservation.get() ?: return
                if (!cancellationScheduled.compareAndSet(false, true)) return
                // Independent scheduling: downstream cancellation must not cancel compensation too.
                Schedulers.boundedElastic().schedule {
                    try {
                        routeBindingService.failSubmission(routeId, taskId,
                            "分析提交已取消，route_id=$routeId, task_id=$taskId")
                    } catch (error: Exception) {
                        logger.error("分析取消补偿失败: routeId=$routeId, taskId=$taskId", error)
                    }
                }
            }
            Mono.fromCallable<KmlAnalysisSubmitRequest> {
                // 不信任客户端taskId；每次显式分析分配新身份，提交重试交由持久边界隔离。
                val input = resolveAnalysisInput(request).copy(taskId = "task_${UUID.randomUUID()}")
                if (delivery.get() == SubmissionDelivery.CANCELLED) return@fromCallable null
                val routeId = routeBindingService.resolveRouteId(input)
                // The transaction may commit after cancel; register first, then check both directions.
                reservation.set(routeId to requireNotNull(input.taskId))
                if (delivery.get() == SubmissionDelivery.CANCELLED) {
                    compensateCancellation()
                    null
                } else input.copy(routeId = routeId)
            }.subscribeOn(Schedulers.boundedElastic()).flatMap { input ->
                val routeId = requireNotNull(input.routeId)
                val taskId = requireNotNull(input.taskId)
                // SSE只是提示；失败不能阻断提交。processing在HTTP前发送，避免盖过早到终态。
                try {
                    sseTaskEventBus.registerTask(taskId)
                    sseTaskEventBus.publish(taskId, SseProgressEvent(
                        taskId = taskId, routeId = routeId, status = "processing", progress = 10,
                        currentStep = "分析任务已提交，正在分析中"
                    ))
                } catch (error: Exception) {
                    logger.warn("分析SSE登记失败: taskId=$taskId", error)
                }
                Mono.defer { kmlAnalysisClientService.submitAnalysis(input) }
                    .switchIfEmpty(Mono.error(IllegalStateException("Agent提交响应为空")))
                    .flatMap { response ->
                        Mono.fromCallable {
                            check(response.taskId == taskId) { "Agent返回的task_id不匹配" }
                            val status = routeBindingService.markSubmitted(routeId, taskId)
                            response.copy(routeId = routeId, status = status.status)
                        }.subscribeOn(Schedulers.boundedElastic())
                    }
                    .onErrorResume { error ->
                        Mono.fromCallable {
                            val message = "分析提交失败，route_id=$routeId, task_id=$taskId"
                            val status = routeBindingService.failSubmission(routeId, taskId, message)
                            // 回调先提交完成时，以已持久化终态为准，补偿不能逆转。
                            if (status.status == "completed") {
                                TaskSubmitResponse(taskId, "completed", "分析结果已保存", 0, routeId)
                            } else {
                                throw BusinessException(
                                    message, "ANALYSIS_SUBMISSION_FAILED", HttpStatus.SERVICE_UNAVAILABLE,
                                    mapOf("route_id" to routeId, "task_id" to taskId), error
                                )
                            }
                        }.subscribeOn(Schedulers.boundedElastic())
                    }
            }.handle<TaskSubmitResponse> { response, sink ->
                // Decide immediately before emission, not at Agent ACK or after downstream onNext.
                if (delivery.compareAndSet(SubmissionDelivery.WAITING, SubmissionDelivery.DELIVERED)) {
                    sink.next(response)
                }
            }.doOnCancel {
                if (delivery.compareAndSet(SubmissionDelivery.WAITING, SubmissionDelivery.CANCELLED)) {
                    compensateCancellation()
                }
            }
        }

    fun getTaskStatus(taskId: String): Mono<TaskStatusResponse> =
        Mono.fromCallable {
            routeBindingService.findTaskStatus(taskId)
                ?: throw BusinessException.notFound("持久分析任务不存在: $taskId")
        }.subscribeOn(Schedulers.boundedElastic()).flatMap { persisted ->
            if (persisted.status in setOf("completed", "failed")) {
                Mono.just(persisted)
            } else {
                // 查询期间回调或补偿可能终结任务，读取Agent后再核验持久态。
                kmlAnalysisClientService.getTaskStatus(taskId).flatMap { remote ->
                    Mono.fromCallable {
                        val latest = routeBindingService.findTaskStatus(taskId)
                            ?: throw BusinessException.notFound("任务已被替代: $taskId")
                        if (latest.status in setOf("completed", "failed") || remote.status in setOf("completed", "failed")) latest else remote
                    }.subscribeOn(Schedulers.boundedElastic())
                }.onErrorResume {
                    Mono.fromCallable {
                        routeBindingService.findTaskStatus(taskId)
                            ?: throw BusinessException.notFound("任务已被替代: $taskId")
                    }.subscribeOn(Schedulers.boundedElastic())
                }
            }
        }

    private fun resolveAnalysisInput(request: KmlAnalysisSubmitRequest): KmlAnalysisSubmitRequest {
        if (!request.kmlContent.isNullOrBlank()) return persistContent(request, request.kmlContent)
        if (!request.kmlSource.isNullOrBlank()) {
            val source = request.kmlSource.trim()
            if (isRemote(source)) return request.copy(kmlSource = source, kmlContent = null)
            val content = kmlStorageService.readStoredContent(source)
                ?: throw IllegalArgumentException("没有可用的位置数据，请重新上传 KML")
            // 旧磁盘输入也转成持久原文引用，不能让提交成功依赖临时文件。
            return persistContent(request, content)
        }
        val routeId = request.routeId?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("没有可用的位置数据，请上传 KML")
        val source = routeMapDataRepository.findById(routeId).orElse(null)?.kmlUrl?.trim()
        if (!source.isNullOrEmpty()) {
            if (isRemote(source)) return request.copy(kmlSource = source, kmlContent = null)
            kmlStorageService.readStoredContent(source)?.let { return persistContent(request, it) }
        }
        routeTrackKmlFactory.synthesizeCurrentTrackKml(routeId)?.let { return persistContent(request, it) }
        throw IllegalArgumentException("路线 $routeId 没有可用的位置数据（KML 文件或有效轨迹点），请上传 KML")
    }

    private fun persistContent(request: KmlAnalysisSubmitRequest, content: String): KmlAnalysisSubmitRequest {
        val stored = kmlStorageService.storeContent(content)
        return request.copy(kmlSource = stored.kmlUrl, kmlContent = content)
    }

    private fun isRemote(source: String): Boolean {
        val uri = try { java.net.URI(source) } catch (_: java.net.URISyntaxException) { return false }
        return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    }
}
