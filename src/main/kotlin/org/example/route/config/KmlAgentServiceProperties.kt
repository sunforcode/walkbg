package org.example.route.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "kml-agent")
class KmlAgentServiceProperties {
    var baseUrl: String = "http://localhost:8001"
    var timeout: Int = 30
    var enabled: Boolean = true
}
