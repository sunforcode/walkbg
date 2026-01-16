package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * WalkBG - 徒步旅行助手后台服务
 *
 * 基于Spring Boot和Kotlin构建的徒步旅行规划助手后台服务
 *
 * 注意：@EnableJpaRepositories 和 @EntityScan 配置已移至 DatabaseConfig
 */
@SpringBootApplication
class WalkbgApplication

fun main(args: Array<String>) {
    runApplication<WalkbgApplication>(*args)
}
