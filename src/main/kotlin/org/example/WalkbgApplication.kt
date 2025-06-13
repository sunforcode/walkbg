package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.autoconfigure.domain.EntityScan

/**
 * WalkBG - 徒步旅行助手后台服务
 *
 * 基于Spring Boot和Kotlin构建的徒步旅行规划助手后台服务
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = ["org.example"])
@EntityScan(basePackages = ["org.example"])
class WalkbgApplication

fun main(args: Array<String>) {
    runApplication<WalkbgApplication>(*args)
}
