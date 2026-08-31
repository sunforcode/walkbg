package org.example.route.service

import org.example.route.config.KmlAgentServiceProperties
import org.example.route.dto.HealthCheckResponse
import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.PoiFilterAgentRequest
import org.example.route.dto.PoiFilterAgentResponse
import org.example.route.dto.PoiResolveAgentRequest
import org.example.route.dto.PoiResolveAgentResponse
import org.example.route.dto.TaskStatusResponse
import org.example.route.dto.TaskSubmitResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class KmlAnalysisClientService(
    private val kmlAgentWebClient: WebClient,
    private val properties: KmlAgentServiceProperties
) {
    private val logger = LoggerFactory.getLogger(KmlAnalysisClientService::class.java)

    fun healthCheck(): Mono<HealthCheckResponse> {
        logger.info("执行 KML Agent Service 健康检查")
        return kmlAgentWebClient.get()
            .uri("/health")
            .retrieve()
            .bodyToMono(HealthCheckResponse::class.java)
            .doOnSuccess { response ->
                logger.info("KML Agent Service 健康检查成功: status=${response.status}, version=${response.version}")
            }
            .doOnError { error ->
                logger.error("KML Agent Service 健康检查失败: ${error.message}")
            }
    }

    fun submitAnalysis(request: KmlAnalysisSubmitRequest): Mono<TaskSubmitResponse> {
        logger.info("提交 KML 分析任务: kmlSource=${request.kmlSource}, routeId=${request.routeId}")
        return kmlAgentWebClient.post()
            .uri("/api/v1/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TaskSubmitResponse::class.java)
            .doOnSuccess { response ->
                logger.info("KML 分析任务提交成功: taskId=${response.taskId}")
            }
            .doOnError { error ->
                logger.error("KML 分析任务提交失败: ${error.message}")
            }
    }

    fun getTaskStatus(taskId: String): Mono<TaskStatusResponse> {
        logger.info("查询任务状态: taskId=$taskId")
        return kmlAgentWebClient.get()
            .uri("/api/v1/tasks/{taskId}", taskId)
            .retrieve()
            .bodyToMono(TaskStatusResponse::class.java)
            .doOnSuccess { response ->
                logger.info("任务状态查询成功: taskId=$taskId, status=${response.status}")
            }
            .doOnError { error ->
                if (error is WebClientResponseException.NotFound) {
                    logger.warn("任务不存在: taskId=$taskId")
                } else {
                    logger.error("任务状态查询失败: ${error.message}")
                }
            }
    }

    /**
     * POI LLM 筛选（同步阻塞调用，LLM 分批处理大列表，需要较长超时）
     */
    fun filterPois(request: PoiFilterAgentRequest): PoiFilterAgentResponse {
        logger.info("提交 POI 筛选请求: routeId=${request.routeId}, 数量=${request.pois.size}")
        return kmlAgentWebClient.post()
            .uri("/api/v1/pois/filter")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PoiFilterAgentResponse::class.java)
            .block(Duration.ofMinutes(5))
            ?: throw IllegalStateException("POI 筛选响应为空")
    }

    /**
     * POI 位置合并 AI 判定（同步阻塞调用，LLM 分批判定，需要较长超时）。
     * 代码不写合并策略：候选召回与是否同一位置的判定都由 Agent 完成。
     */
    fun resolvePoiMatches(request: PoiResolveAgentRequest): PoiResolveAgentResponse {
        logger.info(
            "提交 POI 位置判定请求: routeId=${request.routeId}, " +
                "pois=${request.pois.size}, library=${request.library.size}"
        )
        return kmlAgentWebClient.post()
            .uri("/api/v1/pois/resolve")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PoiResolveAgentResponse::class.java)
            .block(Duration.ofMinutes(5))
            ?: throw IllegalStateException("POI 位置判定响应为空")
    }

    fun isServiceAvailable(): Mono<Boolean> {
        return healthCheck()
            .map { it.status == "healthy" }
            .onErrorReturn(false)
    }
}
