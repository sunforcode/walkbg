package org.example.guide.service

import org.example.common.util.IdGenerator
import org.example.guide.model.Guide
import org.example.guide.repository.GuideRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 攻略领域服务实现
 */
@Service
class GuideServiceImpl(
    private val guideRepository: GuideRepository
) : GuideService {

    @Transactional(readOnly = true)
    override fun getGuides(tag: String?, limit: Int, pageable: Pageable): Page<Guide> {
        // 领域规则：只返回已发布的攻略（status = 1）
        val sort = Sort.by(Sort.Order.desc("createdAt"))
        val pageRequest = PageRequest.of(
            pageable.pageNumber,
            limit.coerceAtMost(100),
            sort
        )

        return if (tag.isNullOrBlank()) {
            // 无标签过滤，返回所有已发布攻略
            guideRepository.findByStatus(1, pageRequest)
        } else {
            // 按标签过滤已发布攻略
            guideRepository.searchGuides(tag, 1, null, pageRequest)
        }
    }

    @Transactional(readOnly = true)
    override fun getGuideById(id: String): Guide? {
        return guideRepository.findById(id).orElse(null)
    }

    @Transactional
    override fun createGuide(guide: Guide): Guide {
        // 领域规则：验证攻略创建
        validateGuideForCreation(guide)

        return guideRepository.save(guide)
    }

    @Transactional
    override fun updateGuide(id: String, guide: Guide): Guide? {
        val existingGuide = guideRepository.findById(id).orElse(null) ?: return null

        // 更新字段
        existingGuide.title = guide.title
        existingGuide.content = guide.content
        existingGuide.coverUrl = guide.coverUrl
        existingGuide.tags = guide.tags
        existingGuide.status = guide.status
        existingGuide.updatedAt = Instant.now()

        return guideRepository.save(existingGuide)
    }

    @Transactional
    override fun deleteGuide(id: String): Boolean {
        if (!guideRepository.existsById(id)) {
            return false
        }
        guideRepository.deleteById(id)
        return true
    }

    @Transactional
    override fun publishGuide(id: String): Guide? {
        val guide = guideRepository.findById(id).orElse(null) ?: return null

        // 领域规则：只有草稿状态的攻略才能发布
        if (guide.status != 0) {
            throw IllegalStateException("只有草稿状态的攻略才能发布")
        }

        guide.publish()
        return guideRepository.save(guide)
    }

    @Transactional
    override fun takeGuideOffline(id: String): Guide? {
        val guide = guideRepository.findById(id).orElse(null) ?: return null

        // 领域规则：只有已发布的攻略才能下线
        if (guide.status != 1) {
            throw IllegalStateException("只有已发布的攻略才能下线")
        }

        guide.takeOffline()
        return guideRepository.save(guide)
    }

    @Transactional
    override fun incrementViewCount(id: String): Guide? {
        val guide = guideRepository.findById(id).orElse(null) ?: return null
        guide.incrementViewCount()
        return guideRepository.save(guide)
    }

    @Transactional
    override fun likeGuide(id: String): Guide? {
        val guide = guideRepository.findById(id).orElse(null) ?: return null
        guide.incrementLikeCount()
        return guideRepository.save(guide)
    }

    @Transactional
    override fun unlikeGuide(id: String): Guide? {
        val guide = guideRepository.findById(id).orElse(null) ?: return null
        guide.decrementLikeCount()
        return guideRepository.save(guide)
    }

    /**
     * 领域规则：验证攻略创建
     */
    private fun validateGuideForCreation(guide: Guide) {
        if (guide.title.isBlank()) {
            throw IllegalArgumentException("攻略标题不能为空")
        }

        if (guide.title.length > 200) {
            throw IllegalArgumentException("攻略标题不能超过200个字符")
        }
    }
}
