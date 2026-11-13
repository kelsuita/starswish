package com.kelsuita.starswish.sync

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import net.lingala.zip4j.ZipFile

internal data class WebDavBackup(val name: String, val modified: Long, val length: Long, val href: String)

internal class WebDavClient(
    private val root: String,
    private val username: String,
    private val password: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    /** Newest `.lch` file in the backup folder, following the server's `rel="next"` pages. */
    suspend fun latestBackup(): WebDavBackup = withContext(Dispatchers.IO) {
        val base = URL(normalizeRoot(root))
        var directory = base.toURI().resolve(BACKUP_FOLDER).toURL()
        val backups = ArrayList<WebDavBackup>()
        val visited = LinkedHashSet<String>()
        while (visited.add(directory.toString())) {
            if (visited.size > MAX_PAGES) throw SyncError("webdav.error_http", "pages")
            val page = request(directory, "PROPFIND", ALL_PROP, "1")
            backups += parseBackups(page.body)
            directory = page.next?.let {
                URL(directory.toURI().resolve(it).toString()).also { next ->
                    requireSameOrigin(base, next)
                }
            } ?: break
        }
        backups.maxByOrNull { it.modified } ?: throw SyncError("webdav.error_no_backup")
    }

    private fun parseBackups(xml: String): List<WebDavBackup> {
        // Android's built-in parser rejects the usual JAXP hardening features, so refuse a DTD
        // outright and never expand entity references instead of relying on those features.
        if (xml.contains("<!DOCTYPE", ignoreCase = true)) throw SyncError("webdav.error_http", "doctype")
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        runCatching { factory.isExpandEntityReferences = false }
        runCatching { factory.isXIncludeAware = false }
        val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val nodes = document.getElementsByTagNameNS(DAV_NS, "response")
        return (0 until nodes.length).mapNotNull { index ->
            val element = nodes.item(index) as? org.w3c.dom.Element ?: return@mapNotNull null
            val href = element.child("href")?.textContent?.trim() ?: return@mapNotNull null
            val prop = element.getElementsByTagNameNS(DAV_NS, "prop").item(0) as? org.w3c.dom.Element
                ?: return@mapNotNull null
            val isCollection = prop.getElementsByTagNameNS(DAV_NS, "resourcetype").item(0)
                ?.let { (it as? org.w3c.dom.Element)?.getElementsByTagNameNS(DAV_NS, "collection")?.length }
                ?: 0
            if (isCollection > 0) return@mapNotNull null
            val name = Uri.decode(href.substringAfterLast('/'))
            if (!name.endsWith(".lch", ignoreCase = true)) return@mapNotNull null
            val modified = prop.getElementsByTagNameNS(DAV_NS, "getlastmodified").item(0)?.textContent.orEmpty()
            val length = prop.getElementsByTagNameNS(DAV_NS, "getcontentlength").item(0)?.textContent
                ?.toLongOrNull() ?: 0L
            val timestamp = runCatching {
                ZonedDateTime.parse(modified, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            }.getOrDefault(0L)
            WebDavBackup(name, timestamp, length, href)
        }
    }

    suspend fun download(backup: WebDavBackup, output: File) = withContext(Dispatchers.IO) {
        if (backup.length !in 1..MAX_BACKUP_BYTES) throw SyncError("webdav.error_download")
        val base = URL(normalizeRoot(root))
        val url = base.toURI().resolve(URI(backup.href)).toURL()
        if (url.protocol != base.protocol || !url.host.equals(base.host, true) || url.port != base.port) {
            throw SyncError("webdav.error_host")
        }
        val request = Request.Builder().url(url).header("Authorization", authorization()).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw SyncError("webdav.error_http", response.code.toString())
            val body = response.body ?: throw SyncError("webdav.error_download")
            body.byteStream().use { input ->
                output.outputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_BACKUP_BYTES) throw SyncError("webdav.error_download")
                        out.write(buffer, 0, count)
                    }
                    if (total != backup.length) throw SyncError("webdav.error_download")
                }
            }
        }
    }

    private data class DavPage(val body: String, val next: String?)

    private fun request(url: URL, method: String, body: String, depth: String): DavPage {
        val request = Request.Builder().url(url)
            .method(method, body.toRequestBody(XML_MEDIA_TYPE.toMediaType()))
            .header("Authorization", authorization())
            .header("Depth", depth)
            .header("User-Agent", "Starwish/1.0")
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code != 207 && !response.isSuccessful) {
                throw if (response.code == 401 || response.code == 403) SyncError("webdav.error_auth")
                else SyncError("webdav.error_http", response.code.toString())
            }
            val body = response.body ?: throw SyncError("webdav.error_http", response.code.toString())
            val text = readLimited(body.byteStream(), MAX_DAV_XML_BYTES)
                .toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
                ?: throw SyncError("webdav.error_http", response.code.toString())
            return DavPage(text, nextLink(response.header("Link")))
        }
    }

    /** Jianguoyun pages long folders and points at the next page with `Link: &lt;url&gt;; rel="next"`. */
    private fun nextLink(header: String?): String? = header
        ?.split(',')
        ?.map(String::trim)
        ?.firstOrNull { it.contains("rel=\"next\"", ignoreCase = true) }
        ?.substringAfter('<', "")
        ?.substringBefore('>')
        ?.takeIf { it.isNotBlank() }

    private fun authorization() = "Basic " + Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray(Charsets.UTF_8))

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
        input.use { stream ->
            val output = ByteArrayOutputStream(minOf(limit, 8192))
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                if (total > limit) throw SyncError("webdav.error_download")
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun requireSameOrigin(base: URL, candidate: URL) {
        if (candidate.protocol != base.protocol ||
            !candidate.host.equals(base.host, ignoreCase = true) ||
            effectivePort(candidate) != effectivePort(base) ||
            candidate.userInfo != null
        ) throw SyncError("webdav.error_host")
    }

    private fun effectivePort(url: URL): Int = if (url.port != -1) url.port else url.defaultPort

    private fun org.w3c.dom.Node.child(name: String) =
        (this as? org.w3c.dom.Element)?.getElementsByTagNameNS(DAV_NS, name)?.item(0)

    private fun normalizeRoot(value: String): String {
        val trimmed = value.trim().ifEmpty { WebDavSettings.DEFAULT_URL }
        val parsed = Uri.parse(trimmed)
        if (parsed.scheme != "https" && parsed.scheme != "http") throw SyncError("webdav.error_url")
        if (parsed.host == null || parsed.userInfo != null) throw SyncError("webdav.error_url")
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }

    companion object {
        private const val DAV_NS = "DAV:"
        private const val BACKUP_FOLDER = "小星记账备份/"
        private const val XML_MEDIA_TYPE = "text/xml; charset=utf-8"
        private const val MAX_BACKUP_BYTES = 256L * 1024 * 1024
        private const val MAX_DAV_XML_BYTES = 4 * 1024 * 1024
        private const val MAX_PAGES = 20
        private const val ALL_PROP = """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:allprop/></d:propfind>"""
    }
}

internal object XiaoxingBackupDecryptor {
    private const val PASSWORD = "EuIDl^FFs29x1sw@"
    private const val MAX_DATABASE_BYTES = 256L * 1024 * 1024

    fun decrypt(archive: File, destination: File): File {
        val zip = ZipFile(archive, PASSWORD.toCharArray())
        val entry = zip.fileHeaders.singleOrNull { !it.isDirectory && it.fileName.matches(Regex("v4\\.0\\.8_db2")) }
            ?: throw SyncError("webdav.error_version")
        if (entry.uncompressedSize !in 1..MAX_DATABASE_BYTES) throw SyncError("webdav.error_download")
        zip.extractFile(entry, requireNotNull(destination.parentFile).absolutePath, destination.name)
        if (!destination.isFile || destination.length() != entry.uncompressedSize) {
            throw SyncError("webdav.error_download")
        }
        return destination
    }
}
