package org.example.route.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.common.exception.BusinessException
import org.example.route.model.*
import org.example.route.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class RoutePublicationService(
    private val routes: RouteRepository,
    private val maps: RouteMapDataRepository,
    private val tags: RouteTagRepository,
    private val images: RouteImageRepository,
    private val segments: SegmentRepository,
    private val pois: PoiPointRepository,
    private val versions: RouteVersionRepository,
    private val versionImages: RouteVersionImageRepository,
    private val orders: RouteVersionPublicationOrderRepository,
    private val current: RouteCurrentPublicVersionRepository,
    private val collection: PublicRouteCollectionRepository,
    private val requests: RoutePublicationRequestRepository,
    private val configuration: RoutePublicationConfigurationRepository,
    private val objectMapper: ObjectMapper,
    private val trackReviews: RouteTrackReviewService,
    private val content: RoutePublicationContentService
) {
    @Transactional
    fun publish(routeId: String, publicRouteType: String?, publicationId: String?): RouteVersion {
        if (publicRouteType !in setOf("one_day", "multi_day")) {
            throw BusinessException.badRequest("必须明确指定公共路线类型 one_day 或 multi_day")
        }
        if (publicationId.isNullOrBlank() || publicationId.length > 64) {
            throw BusinessException.badRequest("发布请求身份必须非空且最多64字符")
        }
        val route = lockRoute(routeId)
        val key = RoutePublicationRequestKey(routeId, publicationId)
        val receipt = requests.findById(key).orElse(null)
        if (receipt != null) {
            if (receipt.publicRouteType != publicRouteType || publishedVersionId(routeId) != receipt.routeVersionId) {
                throw BusinessException.conflict("发布请求已失效或参数不同，请明确发起新的发布操作")
            }
            return versions.findById(receipt.routeVersionId).orElseThrow {
                BusinessException.internalError("发布回执对应的版本不存在")
            }
        }
        if (route.name.isBlank()) {
            throw BusinessException.unprocessableEntity("发布前检查未通过：路线名称为空")
        }
        val sourceSegments = segments.findByRouteId(routeId)
        val sourcePois = pois.findByRouteId(routeId)
        if (sourceSegments.any { it.status == "draft" } || sourcePois.any { it.status == "draft" }) {
            throw BusinessException.unprocessableEntity("发布前检查未通过：仍有未采纳的分段或 POI 草稿")
        }
        // Always lock the route before the global allocator. The singleton is seeded by Flyway.
        val allocator = lockConfiguration()
        val sequence = Math.addExact(orders.findByRouteIdOrderByPublishedSequenceAsc(routeId)
            .maxOfOrNull { it.publishedSequence } ?: 0, 1)
        val map = maps.findById(routeId).orElse(null)
        val mainTrack = trackReviews.publicationTrack(route)
        val versionId = UUID.randomUUID().toString()
        val publishedContent = content.prepare(versionId, route, requireNotNull(publicRouteType), mainTrack, sourceSegments, sourcePois)
        val version = versions.saveAndFlush(RouteVersion(
            id = versionId,
            routeId = routeId,
            routeType = publicRouteType,
            name = route.name,
            region = route.region?.takeIf { it.isNotBlank() },
            introduction = route.description?.takeIf { it.isNotBlank() },
            distanceMeters = map?.distance?.multiply(BigDecimal("1000")),
            ascentMeters = map?.elevationGain,
            descentMeters = map?.elevationLoss,
            tagsJson = objectMapper.writeValueAsString(tags.findByRouteId(routeId).map { it.tag }.filter { it.isNotBlank() }.distinct()),
            mainTrackAvailability = mainTrack.availability,
            mainTrackJson = mainTrack.json,
            mainTrackReferenceSystem = mainTrack.referenceSystem,
            referenceDaysJson = publishedContent.days.takeIf { it.isNotEmpty() }?.let(objectMapper::writeValueAsString)
        ))
        content.persist(publishedContent)
        writeImages(route, version.id)
        orders.saveAndFlush(RouteVersionPublicationOrder(routeId, version.id, sequence))
        current.saveAndFlush(RouteCurrentPublicVersion(routeId, version.id))
        if (!collection.existsById(routeId)) {
            allocator.lastAllRouteOrder = Math.addExact(allocator.lastAllRouteOrder, 1)
            configuration.saveAndFlush(allocator)
            collection.saveAndFlush(PublicRouteCollectionEntry(routeId, allocator.lastAllRouteOrder))
        }
        requests.saveAndFlush(RoutePublicationRequestReceipt(routeId, publicationId, version.id, publicRouteType!!))
        return version
    }

    @Transactional
    fun withdraw(routeId: String) {
        lockRoute(routeId)
        lockConfiguration()
        collection.deleteById(routeId)
        collection.flush()
        current.deleteById(routeId)
        current.flush()
    }

    @Transactional(readOnly = true)
    fun publishedVersionId(routeId: String): String? {
        if (!collection.existsById(routeId)) return null
        val versionId = current.findById(routeId).orElse(null)?.routeVersionId ?: return null
        return versions.findById(versionId).orElse(null)?.takeIf { it.routeId == routeId }?.id
    }

    private fun lockRoute(routeId: String): Route {
        val route = routes.findByIdForUpdate(routeId) ?: throw BusinessException.notFound("路线不存在")
        if (route.status == 3) throw BusinessException.conflict("路线分析中，暂不可变更公开状态")
        return route
    }

    private fun lockConfiguration(): RoutePublicationConfiguration = configuration.findForUpdate()
        ?: throw BusinessException.internalError("公共发布配置未初始化")

    private fun writeImages(route: Route, versionId: String) {
        val source = images.findByRouteIdOrderBySequenceNumber(route.id)
        val cover = route.coverUrl?.takeIf { it.isNotBlank() }
            ?: source.firstOrNull { it.isCover && it.imageUrl.isNotBlank() }?.imageUrl
        val references = (listOfNotNull(cover) + source.map { it.imageUrl }.filter { it.isNotBlank() }).distinct()
        versionImages.saveAllAndFlush(references.mapIndexed { index, reference ->
            RouteVersionImage(UUID.randomUUID().toString(), versionId, reference,
                if (reference == cover) "cover" else "environment", index + 1)
        })
    }
}
