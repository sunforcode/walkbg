package org.example.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * 数据库配置类
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["org.example.repository"])
@EntityScan(basePackages = ["org.example.model"])
@EnableJpaAuditing
@EnableCaching
class DatabaseConfig {

    /**
     * 如果需要自定义数据源，可以取消注释此方法
     * 注意：这将覆盖application.properties中的数据源配置
     */
    /*
    @Bean
    fun dataSource(): DataSource {
        return EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("walkbgdb")
            .build()
    }
    */
}