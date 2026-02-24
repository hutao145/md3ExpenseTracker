package com.example.expensetracker.data.remote

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.text.SimpleDateFormat
import java.util.Locale

data class WebDavFileItem(
    val name: String,
    val size: Long,
    val dateModified: Long
)

class WebDavClient {

    private val client = OkHttpClient()

    /**
     * 测试 WebDAV 连接（使用 PROPFIND 方法）
     */
    fun testConnection(url: String, user: String, pass: String): Boolean {
        return try {
            val credential = Credentials.basic(user, pass)
            val request = Request.Builder()
                .url(url)
                // PROPFIND 带深度请求
                .header("Depth", "0")
                .header("Authorization", credential)
                .method("PROPFIND", null)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 上传文件到 WebDAV (使用 PUT 方法)
     */
    fun uploadFile(url: String, user: String, pass: String, file: File): Boolean {
        return try {
            val credential = Credentials.basic(user, pass)
            val mediaType = "application/octet-stream".toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从 WebDAV 下载文件 (使用 GET 方法)
     */
    fun downloadFile(url: String, user: String, pass: String, destinationFile: File): Boolean {
        return try {
            val credential = Credentials.basic(user, pass)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从 WebDAV 删除文件 (使用 DELETE 方法)
     */
    fun deleteFile(url: String, user: String, pass: String): Boolean {
        return try {
            val credential = Credentials.basic(user, pass)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 404
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 列出 WebDAV 目录下的文件 (使用 PROPFIND 方法，Depth = 1)
     */
    fun listFiles(url: String, user: String, pass: String): List<WebDavFileItem> {
        val result = mutableListOf<WebDavFileItem>()
        try {
            val credential = Credentials.basic(user, pass)
            val request = Request.Builder()
                .url(url)
                .header("Depth", "1")
                .header("Authorization", credential)
                .method("PROPFIND", null)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body ?: return emptyList()

                // 解析 XML 响应
                val factory = DocumentBuilderFactory.newInstance()
                // 支持包含命名空间的查询
                factory.isNamespaceAware = true
                val builder = factory.newDocumentBuilder()
                val document = builder.parse(body.byteStream())

                // WebDAV 标准响应格式 D:multistatus -> D:response
                val responses: NodeList = document.getElementsByTagNameNS("*", "response")

                val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                
                // 跳过第一个响应，通常是目录本身
                for (i in 1 until responses.length) {
                    val responseElement = responses.item(i) as Element
                    
                    // 获取 href (路径)
                    var href = ""
                    val hrefList = responseElement.getElementsByTagNameNS("*", "href")
                    if (hrefList.length > 0) {
                        href = hrefList.item(0).textContent
                    }

                    // 如果是指向一个目录(如带有尾部的斜杠)，直接跳过我们只找文件
                    if (href.endsWith("/")) continue

                    // 提取文件名
                    val decodedHref = java.net.URLDecoder.decode(href, "UTF-8")
                    val name = decodedHref.substringAfterLast("/")

                    // 获取各种属性
                    var size = 0L
                    var dateModified = 0L
                    val propstatList = responseElement.getElementsByTagNameNS("*", "propstat")
                    
                    for (j in 0 until propstatList.length) {
                        val propstat = propstatList.item(j) as Element
                        val statusList = propstat.getElementsByTagNameNS("*", "status")
                        if (statusList.length > 0 && statusList.item(0).textContent.contains("200 OK")) {
                            val propList = propstat.getElementsByTagNameNS("*", "prop")
                            if (propList.length > 0) {
                                val prop = propList.item(0) as Element
                                
                                // 获取文件大小 getcontentlength
                                val lengthList = prop.getElementsByTagNameNS("*", "getcontentlength")
                                if (lengthList.length > 0) {
                                    size = lengthList.item(0).textContent.toLongOrNull() ?: 0L
                                }
                                
                                // 获取修改时间 getlastmodified
                                val lastModList = prop.getElementsByTagNameNS("*", "getlastmodified")
                                if (lastModList.length > 0) {
                                    val dateStr = lastModList.item(0).textContent
                                    try {
                                        dateModified = dateFormat.parse(dateStr)?.time ?: 0L
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    
                    if (name.isNotEmpty() && name.endsWith(".csv", ignoreCase = true)) {
                        result.add(WebDavFileItem(name, size, dateModified))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 按照时间倒序
        return result.sortedByDescending { it.dateModified }
    }
}
