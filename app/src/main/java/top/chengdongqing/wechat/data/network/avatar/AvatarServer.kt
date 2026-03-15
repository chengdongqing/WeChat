package top.chengdongqing.wechat.data.network.avatar

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.core.util.getLocalIpAddress
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * 轻量级的头像服务器
 */
@Singleton
class AvatarServer @Inject constructor(
    private val profileRepository: ProfileRepository,
    @param:IoScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "CallModule"
    }

    private var serverSocket: ServerSocket? = null
    private var serverPort: Int = -1
    val avatarUrl get() = "http://${getLocalIpAddress()}:${serverPort}/avatar?t=${System.currentTimeMillis()}"

    /**
     * 启动服务并返回系统分配的随机端口
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        val socket = ServerSocket(0, 50, InetAddress.getByName("0.0.0.0")).also {
            serverSocket = it
        }
        serverPort = socket.localPort

        scope.launch(Dispatchers.IO) {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()

                    launch {
                        profileRepository.getProfile()?.avatarPath?.let {
                            handleRequest(client, it)
                        }
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }

        Log.d(TAG, "头像服务器已启动")

        return@withContext
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null

        Log.d(TAG, "头像服务器已停止")
    }

    private fun handleRequest(client: Socket, avatarPath: String) {
        client.use { socket ->
            // 设置超时，防止恶意连接占着不放
            socket.soTimeout = 5000

            val input = socket.getInputStream()
            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: return

            // 快速消耗 Header
            while (true) {
                val line = reader.readLine()
                if (line.isNullOrBlank()) break
            }

            val output = socket.getOutputStream().buffered()

            when {
                requestLine.startsWith("GET /avatar") -> {
                    val file = File(avatarPath)
                    if (file.exists()) {
                        val body = buildString {
                            append("HTTP/1.1 200 OK\r\n")
                            append("Content-Type: image/jpeg\r\n")
                            append("Content-Length: ${file.length()}\r\n")
                            append("Connection: close\r\n")
                            append("\r\n")
                        }.toByteArray()

                        output.write(body)
                        file.inputStream().use { it.copyTo(output) }
                    } else {
                        send404(output)
                    }
                }

                // 其他路径返回 404
                else -> send404(output)
            }
            output.flush()
        }
    }
}

private fun send404(out: OutputStream) {
    out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
}