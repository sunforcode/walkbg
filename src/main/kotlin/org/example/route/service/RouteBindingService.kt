package org.example.route.service

import org.example.common.util.IdGenerator
import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.model.Route
import org.example.route.repository.RouteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 路线绑定服务（独立 Bean，确保 @Transactional 通过 Spring AOP 代理生效）
 *
 * RouteAnalysisOrchestrationService 在 Reactor boundedElastic 线程中通过
 * Mono.fromCallable 调用此服务，若在同一个 Bean 内部调用 @Transactional 方法
 * 会绕过 Spring AOP 代理导致事务失效。将事务逻辑拆到此独立 Bean 可解决该问题。
 */
@Service
class RouteBindingService(
    private val routeRepository: RouteRepository
) {
    private val logger = LoggerFactory.getLogger(RouteBindingService::class.java)

    /**
     * 解析并确保 route_id 存在（有事务保证）：
     * - 若请求携带 route_id：校验存在并更新状态为分析中，返回该 id
     * - 若未携带：创建新 Route，返回新 id
     */
    @Transactional
    fun resolveRouteId(request: KmlAnalysisSubmitRequest): String {
        val existingRouteId = request.routeId

        return if (!existingRouteId.isNullOrBlank()) {
            val route = routeRepository.findById(existingRouteId).orElseThrow {
                IllegalArgumentException("指定的路线不存在: $existingRouteId")
            }
            route.markAnalyzing()
            routeRepository.save(route)
            logger.info("绑定已有路线，routeId=$existingRouteId，状态已更新为分析中")
            existingRouteId
        } else {
            val newRoute = createAnalyzingRoute(request)
            logger.info("自动创建新路线，routeId=${newRoute.id}，名称=${newRoute.name}")
            newRoute.id
        }
    }

    private fun createAnalyzingRoute(request: KmlAnalysisSubmitRequest): Route {
        val routeId = IdGenerator.generateIdWithPrefix("route")
        val routeName = if (!request.regionName.isNullOrBlank()) request.regionName else "待补充"

        val route = Route(
            id = routeId,
            name = routeName,
            description = null,
            region = request.regionName,
            difficulty = request.estimatedDifficulty,
            status = 3, // 分析中
            createdBy = "user_1778070406478_l7GczWED", // admin 用户 ID
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = routeRepository.save(route)
        logger.info("新路线已保存到 DB，routeId=${saved.id}")
        return saved
    }
}
