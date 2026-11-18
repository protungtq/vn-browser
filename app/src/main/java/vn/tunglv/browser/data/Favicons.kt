package vn.tunglv.browser.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Icon chính thức của các trang trong lưới truy cập nhanh: tải một lần
 * từ dịch vụ favicon của Google rồi lưu thành file trong bộ nhớ riêng
 * của app, lần sau mở lại hiện ngay không cần mạng.
 */
object FaviconCache {

    private const val MAX_MEMORY_ENTRIES = 32

    private val memoryCache = mutableMapOf<String, Bitmap>()

    /**
     * Lấy icon của trang: bộ nhớ -> file đã lưu -> tải về rồi lưu file.
     * Trả về null khi không có icon (hiện chữ cái thay thế).
     */
    suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        val host = Uri.parse(url).host?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        synchronized(memoryCache) {
            memoryCache[host]?.let { return it }
        }
        return withContext(Dispatchers.IO) {
            val file = cacheFile(context, host)
            if (file.exists() && file.length() > 0) {
                val cached = decodeFile(file)
                if (cached != null) {
                    putMemory(host, cached)
                    return@withContext cached
                }
                // File hỏng thì xóa để tải lại.
                file.delete()
            }
            val bytes = download(host) ?: return@withContext null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            try {
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
            } catch (_: Exception) {
                // Không lưu được thì vẫn hiện icon lần này, lần sau tải lại.
            }
            putMemory(host, bitmap)
            bitmap
        }
    }

    private fun putMemory(host: String, bitmap: Bitmap) {
        synchronized(memoryCache) {
            if (memoryCache.size >= MAX_MEMORY_ENTRIES && host !in memoryCache) {
                memoryCache.entries.firstOrNull()?.let { memoryCache.remove(it.key) }
            }
            memoryCache[host] = bitmap
        }
    }

    private fun decodeFile(file: File): Bitmap? {
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheFile(context: Context, host: String): File {
        val safeName = host.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        return File(File(context.filesDir, "favicons"), "$safeName.png")
    }

    /** Tải icon qua dịch vụ favicon của Google (tự theo chuyển hướng 301 sang gstatic). */
    private fun download(host: String): ByteArray? {
        return try {
            val connection = URL("https://www.google.com/s2/favicons?domain=$host&sz=128")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val contentType = connection.contentType.orEmpty()
            if (!contentType.startsWith("image/")) return null
            connection.inputStream.use { it.readBytes() }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
