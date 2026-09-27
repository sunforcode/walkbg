package org.example.route.service

import org.example.common.exception.BusinessException
import org.example.route.dto.KmlUploadResponse
import org.example.route.model.KmlStoredInput
import org.example.route.repository.KmlStoredInputRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** DB原文是留存依据，静态文件仅兼容既有URL。 */
@Service
class KmlStorageService(
    @Value("\${app.kml.upload-dir:\${user.dir}/uploads/kml}")
    private val uploadDir: String,
    private val inputRepository: KmlStoredInputRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KmlStorageService::class.java)
        private const val MAX_FILE_SIZE = 20L * 1024 * 1024
    }

    @Transactional
    fun store(file: MultipartFile): KmlUploadResponse {
        if (file.isEmpty) throw BusinessException.badRequest("上传文件为空")
        if (file.size > MAX_FILE_SIZE) throw BusinessException.badRequest("文件大小超过 20MB 限制")
        val extension = file.originalFilename.orEmpty().substringAfterLast('.', "").lowercase()
        if (extension !in setOf("kml", "xml")) {
            throw BusinessException.badRequest("仅支持 .kml 或 .xml 格式的 KML 文件")
        }
        return persist(file.bytes)
    }

    @Transactional
    fun storeContent(content: String): KmlUploadResponse = persist(content.toByteArray(Charsets.UTF_8))

    private fun persist(bytes: ByteArray): KmlUploadResponse {
        val content = validate(bytes)
        val filename = "${UUID.randomUUID()}.kml"
        val url = "/static/kml-upload/$filename"
        // 必需的真实仓库依赖；DB写入失败不能退化为仅磁盘成功。
        inputRepository.saveAndFlush(KmlStoredInput(url, content, bytes.size.toLong()))
        try {
            val dir = Paths.get(uploadDir)
            Files.createDirectories(dir)
            Files.write(dir.resolve(filename), bytes)
        } catch (error: Exception) {
            logger.warn("KML 静态副本写入失败，原文已进入持久事务: $url", error)
        }
        return KmlUploadResponse(kmlUrl = url, fileSize = bytes.size.toLong())
    }

    private fun validate(bytes: ByteArray): String {
        if (bytes.isEmpty()) throw BusinessException.badRequest("KML 内容为空")
        if (bytes.size > MAX_FILE_SIZE) throw BusinessException.badRequest("文件大小超过 20MB 限制")
        val content = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (error: java.nio.charset.CharacterCodingException) {
            throw BusinessException.badRequest("KML 内容必须为有效 UTF-8")
        }
        // 安全配置不可降级：配置失败直接终止，不能退回仅检查头部或不安全解析。
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
        val builder = factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> throw SAXException("外部 XML 资源已禁用") }
            setErrorHandler(object : DefaultHandler() {
                override fun error(error: SAXParseException) { throw error }
                override fun fatalError(error: SAXParseException) { throw error }
            })
        }
        val document = try {
            // 使用已严格解码的UTF-8字符；移除解析输入的BOM，但返回及留存的原文不变。
            StringReader(content.removePrefix("\uFEFF")).use { reader ->
                builder.parse(InputSource(reader))
            }
        } catch (error: SAXException) {
            throw BusinessException.badRequest("文件内容不是有效的 KML（XML 不完整或包含禁用的 DTD/实体）")
        }
        if (document.documentElement?.localName != "kml") {
            throw BusinessException.badRequest("文件内容不是有效的 KML（根节点必须为 kml）")
        }
        return content
    }

    @Transactional(readOnly = true)
    fun readStoredContent(kmlUrl: String): String? {
        inputRepository.findById(kmlUrl).orElse(null)?.let { return it.content }
        val filename = kmlUrl.substringAfterLast('/').trim()
        if (filename.isEmpty() || filename.contains("..")) return null
        val uploadedPath = Paths.get(uploadDir).resolve(filename)
        if (Files.exists(uploadedPath)) return Files.readString(uploadedPath)
        if (kmlUrl.startsWith("/static/kml/") || '/' !in kmlUrl) {
            val resource = ClassPathResource("static/kml/$filename")
            if (resource.exists()) return resource.inputStream.bufferedReader().use { it.readText() }
        }
        return null
    }
}
