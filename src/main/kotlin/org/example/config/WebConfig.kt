package org.example.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

@Configuration
@EnableWebMvc
class WebConfig(
    // 给默认值而非强制注入：@WebMvcTest 这类切片测试不扫描 @Component，
    // 若强制依赖会导致这些测试因缺少 CorsProperties 而无法启动上下文。
    // 正常运行时容器仍会注入配置好的实例。
    private val corsProperties: CorsProperties = CorsProperties()
) : WebMvcConfigurer {

    /**
     * KML 上传落盘目录，与 KmlStorageService 保持同一配置项
     */
    @Value("\${app.kml.upload-dir:\${user.dir}/uploads/kml}")
    private lateinit var kmlUploadDir: String

    /**
     * 配置CORS跨域请求，允许的来源由配置项 cors.allowed-origins 决定。
     * 使用 allowedOriginPatterns 而非 allowedOrigins，
     * 以便同时支持 "*" 与带通配符的具体域名。
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(*corsProperties.originPatterns().toTypedArray())
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

    /**
     * 将 StringHttpMessageConverter 的默认字符集改为 UTF-8。
     *
     * 因为本类标注了 @EnableWebMvc，Spring Boot 的 WebMvcAutoConfiguration 整体失效，
     * 转而由 WebMvcConfigurationSupport#addDefaultHttpMessageConverters 注册转换器，
     * 其中使用的是 `StringHttpMessageConverter()` 无参构造，默认字符集为 ISO-8859-1。
     *
     * SSE 场景下响应 Content-Type 为 text/event-stream（不带 charset），
     * StringHttpMessageConverter#writeInternal 取不到 charset 便回退到该默认值，
     * 导致中文被写成 "?"。这里替换为 UTF-8 版本即可修复。
     */
    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.forEachIndexed { index, converter ->
            if (converter is StringHttpMessageConverter) {
                converters[index] = StringHttpMessageConverter(StandardCharsets.UTF_8)
            }
        }
    }
}