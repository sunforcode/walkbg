package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.route.dto.KmlAnalysisSubmitRequest
import org.example.route.dto.KmlUploadResponse
import org.example.route.dto.TaskStatusResponse
import org.example.route.dto.TaskSubmitResponse
import org.example.route.model.Route
import org.example.route.model.RouteMapData
import org.example.route.repository.RouteMapDataRepository
import org.example.route.repository.RouteRepository
import org.example.route.sse.SseProgressEvent
import org.example.route.sse.SseTaskEventBus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.aop.framework.ProxyFactory
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.TransactionDefinition
import reactor.core.publisher.Mono
import java.util.Optional

class RouteAnalysisOrchestrationServiceTest {
    private fun buildService(
        binding: RouteBindingService,
        client: KmlAnalysisClientService,
        maps: RouteMapDataRepository = mock(),
        storage: KmlStorageService = storage(),
        events: SseTaskEventBus = mock(),
        tracks: RouteTrackKmlFactory = mock()
    ) = RouteAnalysisOrchestrationService(binding, client, maps, storage, events, tracks)

    private fun binding(): RouteBindingService = mock<RouteBindingService>().also {
        whenever(it.resolveRouteId(any())).thenReturn("route-1")
        whenever(it.markSubmitted(any(), any())).thenAnswer { invocation ->
            TaskStatusResponse(invocation.getArgument(1), "processing")
        }
    }

    private fun client(): KmlAnalysisClientService = mock<KmlAnalysisClientService>().also {
        whenever(it.submitAnalysis(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<KmlAnalysisSubmitRequest>(0)
            Mono.just(TaskSubmitResponse(requireNotNull(request.taskId), "pending", "submitted", 30))
        }
    }

    private fun storage(): KmlStorageService = mock<KmlStorageService>().also {
        whenever(it.storeContent(any())).thenReturn(KmlUploadResponse("/static/kml-upload/persisted.kml", 11))
    }

    /** Unit mocks have no DB; use Spring's synchronization machinery, not production fallback branches. */
    private fun transactionalBinding(repository: RouteRepository): RouteBindingService {
        val transactions = object : AbstractPlatformTransactionManager() {
            override fun doGetTransaction(): Any = Any()
            override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit
            override fun doCommit(status: DefaultTransactionStatus) = Unit
            override fun doRollback(status: DefaultTransactionStatus) = Unit
        }
        val target = RouteBindingService(repository, mock(), mock())
        val proxy = ProxyFactory(target)
        proxy.isProxyTargetClass = true
        proxy.addAdvice(TransactionInterceptor(transactions, AnnotationTransactionAttributeSource()))
        return proxy.proxy as RouteBindingService
    }

    @Test
    fun `successful agent submission registers task before publishing processing`() {
        val binding = binding()
        val client = client()
        val events = mock<SseTaskEventBus>()
        val response = buildService(binding, client, events = events)
            .submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest(kmlSource = "https://example.test/route.kml")).block()!!

        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        assertEquals(sent.firstValue.taskId, response.taskId)
        assertTrue(response.taskId.isNotBlank())
        val progress = argumentCaptor<SseProgressEvent>()
        inOrder(events) {
            verify(events).registerTask(response.taskId)
            verify(events).publish(eq(response.taskId), progress.capture())
        }
        assertEquals("processing", progress.firstValue.status)
        assertEquals(10, progress.firstValue.progress)
    }

    @Test
    fun `reanalysis fills kml content from stored relative kml url`() {
        val client = client()
        val maps = mock<RouteMapDataRepository>()
        val storage = storage()
        whenever(maps.findById("route-1")).thenReturn(Optional.of(RouteMapData("route-1", kmlUrl = "/static/kml-upload/abc.kml")))
        whenever(storage.readStoredContent("/static/kml-upload/abc.kml")).thenReturn("<kml></kml>")
        buildService(binding(), client, maps, storage).submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest(routeId = "route-1")).block()

        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        assertEquals("route-1", sent.firstValue.routeId)
        assertEquals("<kml></kml>", sent.firstValue.kmlContent)
        verify(storage).storeContent("<kml></kml>")
        assertEquals("/static/kml-upload/persisted.kml", sent.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis with absolute stored kml url passes it as kml source`() {
        val client = client()
        val maps = mock<RouteMapDataRepository>()
        whenever(maps.findById("route-1")).thenReturn(Optional.of(RouteMapData("route-1", kmlUrl = "https://example.test/old.kml")))
        buildService(binding(), client, maps).submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest(routeId = "route-1")).block()
        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        assertEquals("https://example.test/old.kml", sent.firstValue.kmlSource)
        assertNull(sent.firstValue.kmlContent)
    }

    @Test
    fun `reanalysis falls back to synthesized kml from stored track points`() {
        val client = client()
        val tracks = mock<RouteTrackKmlFactory>()
        val storage = storage()
        whenever(tracks.synthesizeCurrentTrackKml("route-1")).thenReturn("<kml><LineString/></kml>")
        buildService(binding(), client, storage = storage, tracks = tracks)
            .submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest(routeId = "route-1")).block()
        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        assertEquals("route-1", sent.firstValue.routeId)
        assertEquals("<kml><LineString/></kml>", sent.firstValue.kmlContent)
        verify(storage).storeContent("<kml><LineString/></kml>")
        // 批准合同要求合成内容也留存，替代旧的不可重读db-track伪URL。
        assertEquals("/static/kml-upload/persisted.kml", sent.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis falls back to track points when stored kml file unreadable`() {
        val client = client()
        val maps = mock<RouteMapDataRepository>()
        val storage = storage()
        val tracks = mock<RouteTrackKmlFactory>()
        whenever(maps.findById("route-1")).thenReturn(Optional.of(RouteMapData("route-1", kmlUrl = "lost-file.kml")))
        whenever(tracks.synthesizeCurrentTrackKml("route-1")).thenReturn("<kml><LineString/></kml>")
        buildService(binding(), client, maps, storage, tracks = tracks)
            .submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest(routeId = "route-1")).block()
        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        assertEquals("<kml><LineString/></kml>", sent.firstValue.kmlContent)
        verify(storage).readStoredContent("lost-file.kml")
        verify(storage).storeContent("<kml><LineString/></kml>")
        assertEquals("/static/kml-upload/persisted.kml", sent.firstValue.kmlSource)
    }

    @Test
    fun `reanalysis rejects route without any usable location data`() {
        val binding = binding()
        val client = client()
        val error = assertThrows(IllegalArgumentException::class.java) {
            buildService(binding, client).submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest(routeId = "route-1")).block()
        }
        assertTrue(error.message!!.contains("没有可用的位置数据"))
        verify(binding, never()).resolveRouteId(any())
        verify(client, never()).submitAnalysis(any())
    }

    @Test
    fun `explicit uploaded reference is read before calling agent`() {
        val client = client()
        val storage = storage()
        whenever(storage.readStoredContent("/static/kml-upload/input.kml")).thenReturn("<kml></kml>")
        buildService(binding(), client, storage = storage).submitAnalysisWithRouteBinding(
            KmlAnalysisSubmitRequest(routeId = "route-1", kmlSource = "/static/kml-upload/input.kml")
        ).block()
        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        assertEquals("<kml></kml>", sent.firstValue.kmlContent)
        verify(storage).storeContent("<kml></kml>")
    }

    @Test
    fun `response includes route id and agent receives preallocated task identity`() {
        val client = client()
        val response = buildService(binding(), client).submitAnalysisWithRouteBinding(
            KmlAnalysisSubmitRequest(kmlSource = "https://example.test/route.kml")
        ).block()!!
        val sent = argumentCaptor<KmlAnalysisSubmitRequest>()
        verify(client).submitAnalysis(sent.capture())
        val mapper = org.example.config.JacksonConfig().objectMapper()
        assertEquals("route-1", mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(response).path("route_id").asText())
        assertEquals(response.taskId, mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(sent.firstValue).path("task_id").asText())
    }

    @Test
    fun `task registration precedes HTTP submission so early callback is visible`() {
        val client = client()
        val events = mock<SseTaskEventBus>()
        buildService(binding(), client, events = events).submitAnalysisWithRouteBinding(
            KmlAnalysisSubmitRequest(kmlSource = "https://example.test/route.kml")
        ).block()
        inOrder(events, client) {
            verify(events).registerTask(any())
            verify(client).submitAnalysis(any())
        }
    }

    @Test
    fun `unusable new input must not create an analyzing route`() {
        val repository = mock<RouteRepository>()
        val client = client()
        assertThrows(IllegalArgumentException::class.java) {
            buildService(transactionalBinding(repository), client).submitAnalysisWithRouteBinding(KmlAnalysisSubmitRequest()).block()
        }
        verify(repository, never()).save(any<Route>())
        verify(client, never()).submitAnalysis(any())
    }

    @Test
    fun `active analysis rejects a second submission before HTTP`() {
        val repository = mock<RouteRepository>()
        val route = Route(id = "route-1", name = "existing", status = 3, createdBy = "user-1")
        whenever(repository.findByIdForUpdate(route.id)).thenReturn(route)
        val client = client()
        val error = assertThrows(BusinessException::class.java) {
            buildService(transactionalBinding(repository), client).submitAnalysisWithRouteBinding(
                KmlAnalysisSubmitRequest(routeId = route.id, kmlSource = "https://example.test/r.kml")
            ).block()
        }
        assertEquals(409, error.httpStatus.value())
        verify(client, never()).submitAnalysis(any())
    }

    @Test
    fun `uncertain transport closes task and restores preanalysis state`() {
        val repository = mock<RouteRepository>()
        val route = Route(id = "route-1", name = "existing", status = 2, createdBy = "user-1")
        whenever(repository.findByIdForUpdate(route.id)).thenReturn(route)
        val client = mock<KmlAnalysisClientService>()
        whenever(client.submitAnalysis(any())).thenReturn(Mono.error(java.util.concurrent.TimeoutException("response lost")))
        val error = assertThrows(BusinessException::class.java) {
            buildService(transactionalBinding(repository), client).submitAnalysisWithRouteBinding(
                KmlAnalysisSubmitRequest(routeId = route.id, kmlSource = "https://example.test/r.kml")
            ).block()
        }
        assertEquals(2, route.status, "传输不确定仍须恢复原来的已关闭状态")
        assertEquals("failed", route.analysisStatus)
        assertTrue(route.analysisTaskId!!.isNotBlank())
        assertTrue(error.message.orEmpty().contains(route.id))
        assertTrue(error.message.orEmpty().contains(route.analysisTaskId!!))
        val details = requireNotNull(error.details)
        assertEquals(route.id, details["route_id"])
        assertEquals(route.analysisTaskId, details["task_id"])
    }

    @Test
    fun `cancelling pending HTTP submission closes its reserved task`() {
        val binding = binding()
        val accepted = java.util.concurrent.CountDownLatch(1)
        val closed = java.util.concurrent.CountDownLatch(1)
        val client = mock<KmlAnalysisClientService>()
        whenever(client.submitAnalysis(any())).thenReturn(Mono.never<TaskSubmitResponse>().doOnSubscribe { accepted.countDown() })
        whenever(binding.failSubmission(any(), any(), any())).thenAnswer {
            closed.countDown()
            TaskStatusResponse(it.getArgument(1), "failed")
        }
        val subscription = buildService(binding, client).submitAnalysisWithRouteBinding(
            KmlAnalysisSubmitRequest(kmlSource = "https://example.test/r.kml")
        ).subscribe()
        try {
            assertTrue(accepted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            subscription.dispose()
            assertTrue(closed.await(5, java.util.concurrent.TimeUnit.SECONDS), "MVC cancellation must compensate the reservation")
            verify(binding).failSubmission(eq("route-1"), any(), any())
        } finally { subscription.dispose() }
    }

    @Test
    fun `cancellation during reservation closes task once reservation commits`() {
        val binding = binding()
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val closed = java.util.concurrent.CountDownLatch(1)
        whenever(binding.resolveRouteId(any())).thenAnswer {
            entered.countDown()
            // Simulate a JDBC commit completing despite the reactive subscriber cancelling.
            var released = false
            while (!released) {
                try { released = release.await(5, java.util.concurrent.TimeUnit.SECONDS) }
                catch (_: InterruptedException) { /* JDBC can finish after cancellation. */ }
            }
            "route-1"
        }
        whenever(binding.failSubmission(any(), any(), any())).thenAnswer {
            closed.countDown()
            TaskStatusResponse(it.getArgument(1), "failed")
        }
        val client = client()
        val subscription = buildService(binding, client).submitAnalysisWithRouteBinding(
            KmlAnalysisSubmitRequest(kmlSource = "https://example.test/r.kml")
        ).subscribe()
        try {
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
            subscription.dispose()
            release.countDown()
            assertTrue(closed.await(5, java.util.concurrent.TimeUnit.SECONDS), "committed reservation must not leak on cancellation")
            verify(binding).failSubmission(eq("route-1"), any(), any())
            verify(client, never()).submitAnalysis(any())
        } finally { release.countDown(); subscription.dispose() }
    }

    @Test
    fun `cancelling from delivered response does not close accepted task`() {
        val binding = binding()
        val received = java.util.concurrent.CountDownLatch(1)
        val cancelling = ThreadLocal.withInitial { false }
        val scheduledDuringCancel = java.util.concurrent.atomic.AtomicInteger()
        val hook = "delivery-cancel-${java.util.UUID.randomUUID()}"
        reactor.core.scheduler.Schedulers.onScheduleHook(hook) { task ->
            if (cancelling.get()) scheduledDuringCancel.incrementAndGet()
            task
        }
        val subscriber = object : reactor.core.publisher.BaseSubscriber<TaskSubmitResponse>() {
            override fun hookOnNext(value: TaskSubmitResponse) {
                cancelling.set(true)
                try { cancel() } finally { cancelling.remove(); received.countDown() }
            }
        }
        try {
            buildService(binding, client()).submitAnalysisWithRouteBinding(
                KmlAnalysisSubmitRequest(kmlSource = "https://example.test/r.kml")
            ).subscribe(subscriber)
            assertTrue(received.await(5, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(0, scheduledDuringCancel.get(), "delivered response cancellation must not even schedule compensation")
            verify(binding, never()).failSubmission(any(), any(), any())
        } finally {
            subscriber.dispose()
            reactor.core.scheduler.Schedulers.resetOnScheduleHook(hook)
        }
    }

    @Test
    fun `persisted terminal task status bypasses remote state`() {
        val binding = binding()
        val client = client()
        val service = buildService(binding, client)
        listOf("completed", "failed").forEach { status ->
            whenever(binding.findTaskStatus("task-1")).thenReturn(TaskStatusResponse("task-1", status))
            assertEquals(status, service.getTaskStatus("task-1").block()!!.status)
        }
        verify(client, never()).getTaskStatus(any())
    }

    @Test
    fun `remote completion cannot claim results before callback commit`() {
        val binding = binding()
        val client = client()
        whenever(binding.findTaskStatus("task-1")).thenReturn(TaskStatusResponse("task-1", "processing"))
        whenever(client.getTaskStatus("task-1")).thenReturn(Mono.just(TaskStatusResponse("task-1", "completed")))
        assertEquals("processing", buildService(binding, client).getTaskStatus("task-1").block()!!.status)
    }

    @Test
    fun `agent submission enforces configured response deadline`() {
        val properties = org.example.route.config.KmlAgentServiceProperties().apply {
            baseUrl = "https://example.test"
            timeout = 1
        }
        val webClient = org.springframework.web.reactive.function.client.WebClient.builder()
            .exchangeFunction { Mono.never() }.build()
        val error = assertThrows(RuntimeException::class.java) {
            KmlAnalysisClientService(webClient, properties)
                .submitAnalysis(KmlAnalysisSubmitRequest(kmlSource = "https://example.test/r.kml"))
                .block(java.time.Duration.ofSeconds(3))
        }
        assertTrue(error.cause is java.util.concurrent.TimeoutException,
            "configured deadline must signal timeout, not rely on caller cancelling block")
    }

    @Test
    fun `multipart defaults admit contracted KML size and preserve explicit limits`() {
        val runner = org.springframework.boot.test.context.runner.WebApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration::class.java))
            .withUserConfiguration(org.example.config.WebConfig::class.java)
        runner.run { context ->
            val config = context.getBean(jakarta.servlet.MultipartConfigElement::class.java)
            assertEquals(20L * 1024 * 1024, config.maxFileSize)
            assertTrue(config.maxRequestSize > config.maxFileSize, "multipart boundary overhead needs room")
        }
        runner.withPropertyValues("spring.servlet.multipart.max-file-size=25MB", "spring.servlet.multipart.max-request-size=26MB")
            .run { context ->
                val config = context.getBean(jakarta.servlet.MultipartConfigElement::class.java)
                assertEquals(25L * 1024 * 1024, config.maxFileSize)
                assertEquals(26L * 1024 * 1024, config.maxRequestSize)
            }
        runner.withPropertyValues("spring.servlet.multipart.enabled=false").run { context ->
            assertTrue(context.getBeansOfType(jakarta.servlet.MultipartConfigElement::class.java).isEmpty())
        }
    }
}
