package org.example.route.service

import org.example.common.exception.BusinessException
import org.example.route.dto.KmlUploadResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

/**
 * KML 文件存储服务（管理端上传）
 *
 * 文件落盘到可配置目录（默认 {user.dir}/uploads/kml），
 * 由 WebConfig 将 /static/kml-upload/ 路径映射到该目录对外提供访问。
 */
@Service
class KmlStorageService(
    @Value("\${app.kml.upload-dir:\${user.dir}/uploads/kml}")
    private val uploadDir: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KmlStorageService::class.java)
        private const val MAX_FILE_SIZE = 20L * 1024 * 1024 // 20MB
        private val ALLOWED_EXTENSIONS = listOf(".kml", ".xml")
    }

    fun store(file: MultipartFile): KmlUploadResponse {
        if (file.isEmpty) {
            throw BusinessException.badRequest("上传文件为空")
        }
        if (file.size > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("文件大小超过 20MB 限制")
        }

        val originalName = file.originalFilename ?: ""
        val extension = originalName.substringAfterLast('.', "").lowercase()
        if (extension !in listOf("kml", "xml")) {
            throw BusinessException.badRequest("仅支持 .kml 或 .xml 格式的 KML 文件")
        }

        val bytes = file.bytes
        // 内容校验：KML 是 XML，开头必须是 "<"，且前 2KB 内出现 <kml 根节点（允许 <?xml 声明在前）
        val head = String(bytes, 0, minOf(bytes.size, 2048), Charsets.UTF_8).trimStart()
        if (!head.startsWith("<") || !head.contains("<kml", ignoreCase = true)) {
            throw BusinessException.badRequest("文件内容不是有效的 KML（未找到 <kml> 根节点）")
        }

        val dir = Paths.get(uploadDir)
        Files.createDirectories(dir)
        val filename = "${UUID.randomUUID()}.kml"
        val target = dir.resolve(filename)
        Files.write(target, bytes)
        logger.info("KML 上传成功: {} ({} bytes)", target, bytes.size)

        return KmlUploadResponse(
            kmlUrl = "/static/kml-upload/$filename",
            fileSize = file.size
        )
    }
}
