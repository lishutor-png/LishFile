package com.example.transfer

import com.example.data.TransferHistoryDao
import com.example.data.TransferHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalTransferClient {

    data class RemoteFile(
        val name: String,
        val size: Long,
        val isEncrypted: Boolean
    )

    suspend fun fetchRemoteFiles(
        peerHostAndPort: String,
        pin: String
    ): Result<List<RemoteFile>> = withContext(Dispatchers.IO) {
        try {
            val formattedHost = if (peerHostAndPort.startsWith("http://")) peerHostAndPort else "http://$peerHostAndPort"
            val url = URL("$formattedHost/api/info?pin=$pin")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("Koneksi gagal: Kode ${conn.responseCode} (Periksa IP & PIN)"))
            }

            val text = conn.inputStream.reader().readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<RemoteFile>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RemoteFile(
                        name = obj.getString("name"),
                        size = obj.getLong("size"),
                        isEncrypted = obj.getBoolean("isEncrypted")
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadRemoteFile(
        peerHostAndPort: String,
        fileName: String,
        pin: String,
        saveDir: File,
        transferHistoryDao: TransferHistoryDao,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val formattedHost = if (peerHostAndPort.startsWith("http://")) peerHostAndPort else "http://$peerHostAndPort"
            val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
            val url = URL("$formattedHost/api/download?file=$encodedName&pin=$pin")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 30000

            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("Gagal mengunduh: Kode ${conn.responseCode}"))
            }

            val totalBytes = conn.contentLengthLong
            if (!saveDir.exists()) saveDir.mkdirs()
            val targetFile = File(saveDir, fileName)

            BufferedInputStream(conn.inputStream, 64 * 1024).use { bis ->
                BufferedOutputStream(FileOutputStream(targetFile), 64 * 1024).use { bos ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var totalRead = 0L
                    while (bis.read(buffer).also { read = it } != -1) {
                        bos.write(buffer, 0, read)
                        totalRead += read
                        onProgress?.invoke(totalRead, totalBytes)
                    }
                    bos.flush()
                }
            }

            transferHistoryDao.insertTransfer(
                TransferHistoryEntity(
                    fileName = fileName,
                    fileSize = targetFile.length(),
                    direction = "RECEIVED",
                    peerAddress = peerHostAndPort,
                    isEncrypted = fileName.endsWith(".lish"),
                    isSuccess = true
                )
            )

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
