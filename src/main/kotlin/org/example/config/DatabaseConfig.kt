package org.example.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * 数据库配置类
 * 
 * 职责：
 * - 启用 JPA Repository 自动扫描（递归扫描 org.example 下所有模块）
 * - 启用 JPA 审计功能
 * - 启用事务管理
 * - 启用缓存功能（Caffeine）
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["org.example"])
@EntityScan(basePackages = ["org.example"])
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
