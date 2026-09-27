package org.example.route.service

import org.example.common.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import jakarta.persistence.EntityManager
import java.nio.file.Files
import java.nio.file.Path

@DataJpaTest(properties = [
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
@Import(KmlStorageService::class)
class KmlStorageServiceTest {
    companion object {
        @TempDir
        @JvmField
        var uploadDir: Path? = null

        @JvmStatic
        @DynamicPropertySource
        fun storageProperties(registry: DynamicPropertyRegistry) {
            registry.add("app.kml.upload-dir") { uploadDir!!.toString() }
        }
    }

    @Autowired
    private lateinit var service: KmlStorageService

    @Autowired
    private lateinit var beanFactory: AutowireCapableBeanFactory

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `readStoredContent reads legacy classpath kml`() {
        val content = service.readStoredContent("/static/kml/wutaishan.kml")

        assertNotNull(content)
        assertTrue(content!!.contains("<kml", ignoreCase = true))
    }

    @Test
    fun `readStoredContent reads legacy bare classpath filename`() {
        val content = service.readStoredContent("wutaishan.kml")

        assertNotNull(content)
        assertTrue(content!!.contains("<kml", ignoreCase = true))
    }

    @Test
    fun `uploaded content survives disk loss and a fresh service instance`() {
        val original = "<kml><Document><name>数据库留存</name></Document></kml>"
        val response = service.store(MockMultipartFile("file", "route.kml", "application/xml", original.toByteArray()))
        entityManager.flush()
        entityManager.clear()
        Files.deleteIfExists(uploadDir!!.resolve(response.kmlUrl.substringAfterLast('/')))

        val freshService = beanFactory.createBean(KmlStorageService::class.java)

        assertEquals(original, freshService.readStoredContent(response.kmlUrl), "不能依赖原进程对象或临时磁盘")
        assertEquals(original.toByteArray().size.toLong(), response.fileSize)
    }

    @Test
    fun `inline input uses the same persistent storage contract`() {
        // RED阶段不引用尚不存在的生产方法，避免编译失败冒充行为失败。
        val storeContent = KmlStorageService::class.java.methods.singleOrNull {
            it.name == "storeContent" && it.parameterTypes.contentEquals(arrayOf(String::class.java))
        }
        assertNotNull(storeContent, "内联输入必须提供与文件上传统一的持久留存入口")
        val original = "<kml><Document><name>内联输入</name></Document></kml>"
        val response = storeContent!!.invoke(service, original) as org.example.route.dto.KmlUploadResponse
        entityManager.flush()
        entityManager.clear()
        Files.deleteIfExists(uploadDir!!.resolve(response.kmlUrl.substringAfterLast('/')))

        val freshService = beanFactory.createBean(KmlStorageService::class.java)
        assertEquals(original, freshService.readStoredContent(response.kmlUrl))
    }

    @Test
    fun `uploaded KML with an unclosed root is rejected`() {
        val file = MockMultipartFile("file", "route.kml", "application/xml", "<kml>".toByteArray())

        val error = assertThrows(BusinessException::class.java) {
            service.store(file)
        }

        assertEquals(400, error.httpStatus.value(), "头部含有 kml 不能代替完整 XML 校验")
    }

    @Test
    fun `inline KML with an unclosed document is rejected`() {
        val error = assertThrows(BusinessException::class.java) {
            service.storeContent("<kml><Document></kml>")
        }

        assertEquals(400, error.httpStatus.value(), "即使存在根闭合标签，内部未闭合也必须拒绝")
    }

    @Test
    fun `valid default namespace KML retains original content`() {
        val original = """<?xml version="1.0" encoding="UTF-8"?><kml xmlns="http://www.opengis.net/kml/2.2"><Document><name>测试 &amp; 原文</name></Document></kml>"""

        val response = service.storeContent(original)

        assertEquals(original, service.readStoredContent(response.kmlUrl), "校验不得重写原文或展开转义")
    }

    @Test
    fun `valid prefixed KML root with UTF8 BOM is accepted`() {
        val original = "\uFEFF" + """<k:kml xmlns:k="http://www.opengis.net/kml/2.2"><k:Document/></k:kml>"""

        val response = service.store(MockMultipartFile("file", "route.kml", "application/xml", original.toByteArray(Charsets.UTF_8)))

        assertEquals(original, service.readStoredContent(response.kmlUrl))
    }

    @Test
    fun `nested KML element does not make a non KML root valid`() {
        val error = assertThrows(BusinessException::class.java) {
            service.storeContent("<document><kml/></document>")
        }

        assertEquals(400, error.httpStatus.value())
    }

    @Test
    fun `KML with internal DTD is rejected`() {
        val error = assertThrows(BusinessException::class.java) {
            service.storeContent("""<!DOCTYPE kml [<!ENTITY name "expanded">]><kml><Document><name>&name;</name></Document></kml>""")
        }

        assertEquals(400, error.httpStatus.value(), "不允许DTD，即使不引用外部资源")
    }

    @Test
    fun `KML with external DTD is rejected`() {
        val error = assertThrows(BusinessException::class.java) {
            service.storeContent("""<!DOCTYPE kml SYSTEM "https://example.invalid/blocked.dtd"><kml/>""")
        }

        assertEquals(400, error.httpStatus.value())
    }

    @Test
    fun `KML with external entity is rejected`() {
        val error = assertThrows(BusinessException::class.java) {
            service.storeContent("""<!DOCTYPE kml [<!ENTITY external SYSTEM "file:///nonexistent-walk-test-entity">]><kml><Document><name>&external;</name></Document></kml>""")
        }

        assertEquals(400, error.httpStatus.value())
    }
}
