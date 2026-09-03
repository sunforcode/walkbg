package org.example.route.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * KML Agent 服务的连接配置。
 *
 * baseUrl 故意不设默认值：该服务是独立部署的进程，地址完全由环境决定
 * （单机 Compose 下为服务名，拆分主机后为内网地址）。
 * 若源码内保留 localhost 默认值，配置缺失时会退化为连接本机而非启动失败，
 * 表现为运行期超时，难以定位。
 */
@Component
@ConfigurationProperties(prefix = "kml-agent")
class KmlAgentServiceProperties {
    lateinit var baseUrl: String
    var timeout: Int = 30
    var enabled: Boolean = true
}
