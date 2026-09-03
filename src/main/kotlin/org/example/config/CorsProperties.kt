package org.example.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 跨域来源配置。
 *
 * 允许的来源由部署环境决定：同源反向代理部署时前后端同域，
 * 前后端分域名部署时需列出具体域名。因此这里必须可配置，
 * 而不是在多处源码里写死通配符。
 *
 * 值为逗号分隔的来源列表，支持通配符形式的域名。
 */
@Component
@ConfigurationProperties(prefix = "cors")
class CorsProperties {
    // 逗号分隔的允许来源；单个通配符表示不限制。
    var allowedOrigins: String = "*"

    // 解析为列表形式，供 Spring CORS 配置使用。
    fun originPatterns(): List<String> =
        allowedOrigins.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("*") }
}
