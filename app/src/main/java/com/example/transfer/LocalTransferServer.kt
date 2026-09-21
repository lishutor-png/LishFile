package com.example.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.data.TransferHistoryDao
import com.example.data.TransferHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Locale

class LocalTransferServer(
    private val context: Context,
    private val transferHistoryDao: TransferHistoryDao,
    private val incomingDir: File
) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    data class ServerState(
        val isRunning: Boolean = false,
        val ipAddress: String = "",
        val port: Int = 8989,
        val pin: String = "",
        val sharedFiles: List<File> = emptyList(),
        val statusMessage: String = "Server mati"
    )

    private val _state = MutableStateFlow(ServerState())
    val state: StateFlow<ServerState> = _state.asStateFlow()

    fun startServer(sharedFiles: List<File> = emptyList(), customPort: Int = 8989) {
        if (_state.value.isRunning) return

        val pin = String.format(Locale.US, "%04d", SecureRandom().nextInt(10000))
        val localIp = getLocalIpAddress()

        try {
            serverSocket = ServerSocket(customPort)
        } catch (e: Exception) {
            // Port in use, fallback to any available port
            try {
                serverSocket = ServerSocket(0)
            } catch (e2: Exception) {
                _state.value = _state.value.copy(statusMessage = "Gagal memulai server: ${e2.message}")
                return
            }
        }

        val port = serverSocket?.localPort ?: customPort
        _state.value = ServerState(
            isRunning = true,
            ipAddress = localIp,
            port = port,
            pin = pin,
            sharedFiles = sharedFiles,
            statusMessage = "Server aktif di $localIp:$port"
        )

        serverJob = scope.launch {
            while (isActive && serverSocket?.isClosed == false) {
                try {
                    val client = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _state.value = ServerState(isRunning = false, statusMessage = "Server dihentikan")
    }

    fun updateSharedFiles(files: List<File>) {
        _state.value = _state.value.copy(sharedFiles = files)
    }

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        val clientIp = client.inetAddress.hostAddress ?: "Unknown"
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val output = client.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext
            val method = parts[0]
            val fullUrl = parts[1]

            val headers = mutableMapOf<String, String>()
            var line: String?
            var contentLength = 0L
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val colonIdx = line!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase(Locale.ROOT)
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toLongOrNull() ?: 0L
                    }
                }
            }

            val path = if (fullUrl.contains("?")) fullUrl.substringBefore("?") else fullUrl
            val queryStr = if (fullUrl.contains("?")) fullUrl.substringAfter("?") else ""
            val queryParams = parseQueryParams(queryStr)

            when {
                path == "/" -> {
                    serveWebPage(output, _state.value.sharedFiles, _state.value.pin)
                }
                path == "/api/info" -> {
                    val enteredPin = queryParams["pin"]
                    if (enteredPin != _state.value.pin) {
                        sendHttpResponse(output, 403, "Forbidden", "text/plain", "PIN tidak valid.")
                    } else {
                        val fileListJson = _state.value.sharedFiles.joinToString(prefix = "[", postfix = "]") { f ->
                            """{"name":"${f.name}","size":${f.length()},"isEncrypted":${f.name.endsWith(".lish")}}"""
                        }
                        sendHttpResponse(output, 200, "OK", "application/json", fileListJson)
                    }
                }
                path == "/api/download" -> {
                    val fileName = queryParams["file"]
                    val enteredPin = queryParams["pin"]
                    if (enteredPin != _state.value.pin) {
                        sendHttpResponse(output, 403, "Forbidden", "text/plain", "PIN tidak valid.")
                        return@withContext
                    }
                    val targetFile = _state.value.sharedFiles.find { it.name == fileName }
                    if (targetFile != null && targetFile.exists()) {
                        sendFileStream(output, targetFile)
                        // Record in transfer history
                        transferHistoryDao.insertTransfer(
                            TransferHistoryEntity(
                                fileName = targetFile.name,
                                fileSize = targetFile.length(),
                                direction = "SENT",
                                peerAddress = clientIp,
                                isEncrypted = targetFile.name.endsWith(".lish"),
                                isSuccess = true
                            )
                        )
                    } else {
                        sendHttpResponse(output, 404, "Not Found", "text/plain", "File tidak ditemukan")
                    }
                }
                path == "/api/upload" && method == "POST" -> {
                    val enteredPin = queryParams["pin"] ?: headers["x-lish-pin"]
                    val fileName = queryParams["name"] ?: "received_file_${System.currentTimeMillis()}"
                    if (enteredPin != _state.value.pin) {
                        sendHttpResponse(output, 403, "Forbidden", "text/plain", "PIN salah.")
                        return@withContext
                    }

                    if (!incomingDir.exists()) incomingDir.mkdirs()
                    val targetFile = File(incomingDir, fileName)

                    // Read input stream
                    val bis = BufferedInputStream(client.getInputStream())
                    val bos = BufferedOutputStream(FileOutputStream(targetFile))
                    val buf = ByteArray(64 * 1024)
                    var totalRead = 0L
                    var r = bis.read(buf, 0, minOf(buf.size.toLong(), contentLength - totalRead).toInt())
                    while (totalRead < contentLength && r != -1) {
                        bos.write(buf, 0, r)
                        totalRead += r
                        if (totalRead >= contentLength) break
                        r = bis.read(buf, 0, minOf(buf.size.toLong(), contentLength - totalRead).toInt())
                    }
                    bos.flush()
                    bos.close()

                    transferHistoryDao.insertTransfer(
                        TransferHistoryEntity(
                            fileName = targetFile.name,
                            fileSize = targetFile.length(),
                            direction = "RECEIVED",
                            peerAddress = clientIp,
                            isEncrypted = targetFile.name.endsWith(".lish"),
                            isSuccess = true
                        )
                    )

                    sendHttpResponse(output, 200, "OK", "text/plain", "File berhasil diunggah!")
                }
                else -> {
                    sendHttpResponse(output, 404, "Not Found", "text/plain", "Halaman tidak ditemukan")
                }
            }
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun sendFileStream(output: OutputStream, file: File) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: ${file.length()}\r\n" +
                "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))

        BufferedInputStream(FileInputStream(file)).use { fis ->
            val buf = ByteArray(64 * 1024)
            var len: Int
            while (fis.read(buf).also { len = it } != -1) {
                output.write(buf, 0, len)
            }
            output.flush()
        }
    }

    private fun sendHttpResponse(output: OutputStream, code: Int, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val response = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveWebPage(output: OutputStream, files: List<File>, pin: String) {
        val fileItemsHtml = if (files.isEmpty()) {
            "<div class='empty'>Belum ada file yang dipilih untuk dibagikan. Pilih file dari aplikasi File Manager +.</div>"
        } else {
            files.joinToString("\n") { f ->
                val sizeFormatted = com.example.model.StorageStats.formatBytes(f.length())
                val isEnc = f.name.endsWith(".lish")
                val badge = if (isEnc) "<span class='badge enc'>Terenkripsi AES-256</span>" else "<span class='badge'>Biasa</span>"
                """
                <div class="file-item">
                    <div class="file-info">
                        <strong>${f.name}</strong>
                        <div>$sizeFormatted • $badge</div>
                    </div>
                    <a class="btn" href="/api/download?file=${f.name}&pin=$pin" download>Unduh</a>
                </div>
                """.trimIndent()
            }
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>File Manager + Local Share</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; background: #f0f5fa; color: #1e293b; padding: 24px 16px; }
                    .card { max-width: 540px; margin: 0 auto; background: white; border-radius: 16px; padding: 24px; box-shadow: 0 4px 16px rgba(0,0,0,0.06); border: 1px solid #dce8f2; }
                    .header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
                    .header h1 { font-size: 22px; margin: 0; color: #2c6cb0; }
                    .pin-banner { background: #e3f2fd; border-radius: 12px; padding: 12px 16px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
                    .pin-box { font-size: 24px; font-weight: bold; letter-spacing: 4px; color: #1e3a8a; }
                    .file-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #eef2f6; }
                    .btn { background: #2c6cb0; color: white; padding: 8px 16px; border-radius: 8px; text-decoration: none; font-size: 14px; font-weight: 500; }
                    .badge { font-size: 11px; padding: 2px 8px; border-radius: 6px; background: #e2e8f0; color: #475569; }
                    .badge.enc { background: #d1fae5; color: #065f46; font-weight: bold; }
                    .upload-section { margin-top: 24px; padding-top: 20px; border-top: 1px solid #e2e8f0; }
                    .empty { color: #64748b; font-size: 14px; text-align: center; padding: 24px 0; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1>📁 File Manager + Local Transfer</h1>
                    </div>
                    <div class="pin-banner">
                        <span>PIN Sesi Transfer:</span>
                        <span class="pin-box">$pin</span>
                    </div>
                    <h3>File yang Dibagikan</h3>
                    $fileItemsHtml
                    <div class="upload-section">
                        <h4>Kirim File ke Perangkat Ini</h4>
                        <input type="file" id="fileInput" />
                        <button class="btn" style="margin-top:8px; border:none; cursor:pointer;" onclick="uploadFile()">Kirim ke File Manager +</button>
                        <p id="uploadStatus" style="font-size:13px; color:#2563eb;"></p>
                    </div>
                </div>
                <script>
                    function uploadFile() {
                        const fileInput = document.getElementById('fileInput');
                        const status = document.getElementById('uploadStatus');
                        if (!fileInput.files.length) return alert('Pilih file terlebih dahulu');
                        const file = fileInput.files[0];
                        status.innerText = 'Mengunggah file ' + file.name + '...';
                        fetch('/api/upload?pin=' + encodeURIComponent('$pin') + '&name=' + encodeURIComponent(file.name), {
                            method: 'POST',
                            body: file
                        }).then(r => r.text()).then(t => {
                            status.innerText = 'Sukses: ' + t;
                            fileInput.value = '';
                        }).catch(e => {
                            status.innerText = 'Gagal: ' + e;
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        sendHttpResponse(output, 200, "OK", "text/html", html)
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (query.isBlank()) return map
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                map[key] = java.net.URLDecoder.decode(value, "UTF-8")
            }
        }
        return map
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
