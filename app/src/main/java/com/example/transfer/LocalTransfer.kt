package com.example.transfer

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.TransferItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.UUID

class LocalTransferServer(private val port: Int = 8080) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val sharedFiles = mutableListOf<File>()

    private val _serverStatus = MutableStateFlow<String>("Offline")
    val serverStatus: StateFlow<String> = _serverStatus.asStateFlow()

    fun setFiles(files: List<File>) {
        sharedFiles.clear()
        sharedFiles.addAll(files)
    }

    suspend fun startServer(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext true
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            val ip = getDeviceIpAddress(context)
            _serverStatus.value = "Aktif di http://$ip:$port"

            Thread {
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }.start()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _serverStatus.value = "Gagal memulai: ${e.message}"
            isRunning = false
            false
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        _serverStatus.value = "Offline"
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine() ?: return@Thread
                val parts = line.split(" ")
                if (parts.size < 2) return@Thread
                val method = parts[0]
                val path = parts[1]

                val output: OutputStream = socket.getOutputStream()

                when {
                    path == "/" || path == "/api/files" -> {
                        val jsonArray = JSONArray()
                        sharedFiles.forEach { file ->
                            val obj = JSONObject()
                            obj.put("name", file.name)
                            obj.put("size", file.length())
                            obj.put("isDirectory", file.isDirectory)
                            jsonArray.put(obj)
                        }
                        val body = jsonArray.toString()
                        val response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Content-Length: ${body.toByteArray().size}\r\n\r\n$body"
                        output.write(response.toByteArray())
                        output.flush()
                    }
                    path.startsWith("/api/download/") -> {
                        val fileName = java.net.URLDecoder.decode(path.removePrefix("/api/download/"), "UTF-8")
                        val file = sharedFiles.find { it.name == fileName }
                        if (file != null && file.exists() && !file.isDirectory) {
                            val header = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/octet-stream\r\n" +
                                    "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                                    "Content-Length: ${file.length()}\r\n\r\n"
                            output.write(header.toByteArray())
                            FileInputStream(file).use { fis ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (fis.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                }
                            }
                            output.flush()
                        } else {
                            val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                            output.write(notFound.toByteArray())
                            output.flush()
                        }
                    }
                    else -> {
                        val badReq = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"
                        output.write(badReq.toByteArray())
                        output.flush()
                    }
                }
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun getDeviceIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            return "127.0.0.1"
        }
    }
}

class LocalTransferClient {
    suspend fun fetchRemoteFileList(host: String, port: Int = 8080): List<JSONObject> = withContext(Dispatchers.IO) {
        val result = mutableListOf<JSONObject>()
        try {
            val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
            val url = URL("http://$cleanHost:$port/api/files")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = reader.readText()
                val jsonArray = JSONArray(content)
                for (i in 0 until jsonArray.length()) {
                    result.add(jsonArray.getJSONObject(i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun downloadRemoteFile(
        host: String,
        port: Int = 8080,
        fileName: String,
        targetDir: File,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
            val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
            val url = URL("http://$cleanHost:$port/api/download/$encodedName")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val totalLength = conn.contentLength.toLong()
                targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalLength > 0) {
                                onProgress?.invoke(downloaded.toFloat() / totalLength)
                            }
                        }
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
