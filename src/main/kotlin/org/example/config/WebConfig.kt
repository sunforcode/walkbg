package org.example.config

import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
class WebConfig : WebMvcConfigurer {

    /**
     * KML 上传落盘目录，与 KmlStorageService 保持同一配置项
     */
    @Value("\${app.kml.upload-dir:\${user.dir}/uploads/kml}")
    private lateinit var kmlUploadDir: String

    /**
     * 配置CORS跨域请求
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600)
    }

    /**
     * 配置静态资源处理
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
        // KML 上传目录映射（管理端上传的 KML 存放在文件系统而非 classpath，重建不影响历史文件）
        registry.addResourceHandler("/static/kml-upload/**")
            .addResourceLocations("file:${kmlUploadDir.trimEnd('/')}/")
        // 添加Swagger UI资源处理（如果将来添加Swagger）
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")

        // 确保能访问H2控制台
        registry.addResourceHandler("/h2-console/**")
            .addResourceLocations("classpath:/h2-console/")
    }
}