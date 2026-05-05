package org.example.route.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    private val kmlAgentProperties: KmlAgentServiceProperties
) {
    @Bean
    fun kmlAgentWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(kmlAgentProperties.baseUrl)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
            }
            .build()
    }
}
