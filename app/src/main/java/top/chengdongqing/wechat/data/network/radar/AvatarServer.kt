package top.chengdongqing.wechat.data.network.radar

import android.util.Log
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class AvatarServer @Inject constructor() {

    private var serverSocket: ServerSocket? = null

    /**
     * 启动服务并返回系统分配的随机端口
     */
    suspend fun start(scope: CoroutineScope, avatarPath: String): Int =
        withContext(Dispatchers.IO) {
            val socket = ServerSocket(0, 50, InetAddress.getByName("0.0.0.0")).also {
                serverSocket = it
                Log.d("AvatarServer", "服务器已启动，端口: ${it.localPort}, 路径: $avatarPath")
            }
            val assignedPort = socket.localPort

            scope.launch(Dispatchers.IO) {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        Log.d("AvatarServer", "收到连接: ${client.inetAddress}")
                        launch { handleRequest(client, avatarPath) }
                    } catch (e: Exception) {
                        Log.e("AvatarServer", "accept 异常: $e")
                        break
                    }
                }
            }

            return@withContext assignedPort
        }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
    }

    private fun handleRequest(client: Socket, avatarPath: String) {
        client.use {
            val reader = it.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: return // 例如: "GET /hello HTTP/1.1"

            // 消耗掉剩余的请求头 (重要：防止 Socket 提前关闭)
            var line: String? = reader.readLine()
            while (!line.isNullOrBlank()) {
                line = reader.readLine()
            }

            val output = it.getOutputStream()

            when {
                // 测试路由：返回纯文本
                requestLine.contains("GET /hello") -> {
                    val body = "Hello from Radar Server! Time: ${System.currentTimeMillis()}"
                    val response = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: text/plain\r\n")
                        append("Content-Length: ${body.length}\r\n")
                        append("Connection: close\r\n")
                        append("\r\n")
                        append(body)
                    }
                    output.write(response.toByteArray())
                }

                // 头像路由：返回图片文件
                requestLine.contains("GET /avatar") -> {
                    val file = File(avatarPath)
                    if (file.exists()) {
                        val header = buildString {
                            append("HTTP/1.1 200 OK\r\n")
                            append("Content-Type: image/jpeg\r\n")
                            append("Content-Length: ${file.length()}\r\n")
                            append("Connection: close\r\n")
                            append("\r\n")
                        }
                        output.write(header.toByteArray())
                        file.inputStream().use { it.copyTo(output) }
                    } else {
                        output.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                    }
                }

                // 其他路径返回 404
                else -> {
                    output.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                }
            }
            output.flush()
        }
    }
}