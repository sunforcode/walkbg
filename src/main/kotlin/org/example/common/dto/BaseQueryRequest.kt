package org.example.common.dto

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

/**
 * 基础查询请求DTO
 */
open class BaseQueryRequest(
    @field:Min(value = 0, message = "页码不能小于0")
    val page: Int = 0,
    
    @field:Min(value = 1, message = "每页大小不能小于1")
    @field:Max(value = 100, message = "每页大小不能超过100")
    val size: Int = 10,
    
    val sortBy: String = "createdAt",
    val sortDir: String = "desc"
) {
    /**
     * 转换为Pageable对象
     */
    fun toPageable(): Pageable {
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        return PageRequest.of(page, size, sort)
    }
    
    /**
     * 转换为Pageable对象（指定排序字段）
     */
    fun toPageable(defaultSortBy: String): Pageable {
        val actualSortBy = if (sortBy.isBlank()) defaultSortBy else sortBy
        val sort = if (sortDir.lowercase() == "desc") {
            Sort.by(actualSortBy).descending()
        } else {
            Sort.by(actualSortBy).ascending()
        }
        return PageRequest.of(page, size, sort)
    }
}