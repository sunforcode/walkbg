package org.example.guide.service

import org.example.guide.model.Guide
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 攻略领域服务接口
 * 职责：领域业务逻辑、业务规则验证、领域对象操作
 */
interface GuideService {

    /**
     * 获取攻略列表
     * @param tag 标签过滤（可选）
     * @param limit 数量限制
     * @param pageable 分页参数
     */
    fun getGuides(tag: String?, limit: Int, pageable: Pageable): Page<Guide>

    /**
     * 根据ID获取攻略详情
     */
    fun getGuideById(id: String): Guide?

    /**
     * 创建攻略
     */
    fun createGuide(guide: Guide): Guide

    /**
     * 更新攻略
     */
    fun updateGuide(id: String, guide: Guide): Guide?

    /**
     * 删除攻略
     */
    fun deleteGuide(id: String): Boolean

    /**
     * 发布攻略
     */
    fun publishGuide(id: String): Guide?

    /**
     * 下线攻略
     */
    fun takeGuideOffline(id: String): Guide?

    /**
     * 增加浏览次数
     */
    fun incrementViewCount(id: String): Guide?

    /**
     * 点赞攻略
     */
    fun likeGuide(id: String): Guide?

    /**
     * 取消点赞
     */
    fun unlikeGuide(id: String): Guide?
}
