package org.example.route.service

import org.example.common.exception.BusinessException
import org.example.common.util.IdGenerator
import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.TaskStatusResponse
import org.example.route.model.Route
import org.example.route.model.RouteMapData
import org.example.route.repository.RouteMapDataRepository
import org.example.route.repository.RouteRepository
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.example.route.sse.publishAnalysisEventAfterCommit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Short, independent transactions. HTTP submission must never hold this route lock. */
@Service
class RouteBindingService(
    private val routeRepository: RouteRepository,
    private val routeMapDataRepository: RouteMapDataRepository,
    private val eventBus: SseTaskEventBus
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resolveRouteId(request: KmlAnalysisSubmitRequest): String {
        val taskId = requireNotNull(request.taskId) { "提交前必须预分配 task_id" }
        require(taskId.isNotBlank() && taskId.length <= 64) { "task_id 无效" }
        require(!request.kmlSource.isNullOrBlank()) { "分析输入未留存或解析" }
        val route = if (!request.routeId.isNullOrBlank()) {
            routeRepository.findByIdForUpdate(request.routeId)
                ?: throw BusinessException.notFound("指定的路线不存在: ${request.routeId}")
        } else {
            Route(
                id = IdGenerator.generateIdWithPrefix("route"),
                name = request.regionName?.takeIf { it.isNotBlank() } ?: "待补充",
                region = request.regionName,
                difficulty = request.estimatedDifficulty,
                status = 0,
                createdBy = "user_1778070406478_l7GczWED"
            )
        }
        if (route.hasActiveAnalysis()) {
            throw BusinessException.conflict(
                "路线已有未结束分析任务",
                details = mapOf("route_id" to route.id, "task_id" to (route.analysisTaskId ?: ""))
            )
        }
        route.beginAnalysis(taskId)
        routeRepository.save(route)
        val input = routeMapDataRepository.findById(route.id).orElse(null)
            ?: RouteMapData(id = route.id)
        routeMapDataRepository.save(input.copy(kmlUrl = request.kmlSource, updatedAt = Instant.now()))
        return route.id
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markSubmitted(routeId: String, taskId: String): TaskStatusResponse {
        val route = routeRepository.findByIdForUpdate(routeId)
            ?: return TaskStatusResponse(taskId, "failed", error = "路线已删除")
        if (route.acceptsAnalysisResult(taskId)) {
            route.analysisStatus = "processing"
            route.updatedAt = Instant.now()
            routeRepository.save(route)
        }
        return snapshot(route, taskId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun failSubmission(routeId: String, taskId: String, error: String): TaskStatusResponse {
        val route = routeRepository.findByIdForUpdate(routeId)
            ?: return TaskStatusResponse(taskId, "failed", error = "路线已删除")
        if (route.finishAnalysis(taskId, succeeded = false, error = error)) {
            routeRepository.save(route)
            publishAnalysisEventAfterCommit(eventBus, SseProgressEvent(
                taskId = taskId, routeId = routeId, status = "failed", progress = 100,
                currentStep = "分析提交失败", error = error
            ))
        }
        return snapshot(route, taskId)
    }

    @Transactional(readOnly = true)
    fun findTaskStatus(taskId: String): TaskStatusResponse? =
        routeRepository.findByAnalysisTaskId(taskId)?.let { snapshot(it, taskId) }

    private fun snapshot(route: Route, taskId: String): TaskStatusResponse {
        if (route.analysisTaskId != taskId) {
            return TaskStatusResponse(taskId, "failed", progress = 100, error = "任务已被更新的分析替代")
        }
        val status = checkNotNull(route.analysisStatus)
        return TaskStatusResponse(
            taskId = taskId,
            status = status,
            progress = if (status in setOf("completed", "failed")) 100 else 0,
            message = if (status == "completed") "分析结果已保存" else "分析任务$status",
            error = route.analysisError
        )
    }
}
