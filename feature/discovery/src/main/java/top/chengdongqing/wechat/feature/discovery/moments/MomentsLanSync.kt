package top.chengdongqing.wechat.feature.discovery.moments

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MomentsLanSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MomentsRepository,
    private val profileRepository: ProfileRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var mediaServerJob: Job? = null
    private var socket: DatagramSocket? = null
    private var mediaServer: ServerSocket? = null

    fun start() {
        if (job?.isActive == true) return
        repository.onLocalChange = ::broadcast
        startMediaServer()
        broadcast(repository.state.value)
        job = scope.launch {
            runCatching {
                DatagramSocket(null).also {
                    socket = it
                    it.reuseAddress = true
                    it.broadcast = true
                    it.bind(InetSocketAddress(PORT))
                }.use { receiver ->
                    val buffer = ByteArray(MAX_PACKET)
                    while (isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        receiver.receive(packet)
                        val envelope = json.decodeFromString<MomentsEnvelope>(
                            packet.data.decodeToString(0, packet.length)
                        )
                        if (envelope.senderId != profileRepository.requireUserId()) {
                            val localWasNewer = repository.state.value.version > envelope.state.version
                            val host = packet.address.hostAddress ?: continue
                            repository.mergeRemote(
                                downloadRemoteMedia(
                                    envelope.state,
                                    host
                                ),
                                envelope.senderId
                            )
                            if (localWasNewer) broadcast(repository.state.value)
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "朋友圈局域网同步已停止", it) }
        }
    }

    fun stop() {
        repository.onLocalChange = null
        socket?.close()
        socket = null
        mediaServer?.close()
        mediaServer = null
        mediaServerJob?.cancel()
        mediaServerJob = null
        job?.cancel()
        job = null
    }

    private fun broadcast(state: MomentsState) {
        scope.launch {
            runCatching {
                val body = json.encodeToString(
                    MomentsEnvelope(profileRepository.requireUserId(), state)
                ).encodeToByteArray()
                if (body.size > MAX_PACKET) return@runCatching
                DatagramSocket().use {
                    it.broadcast = true
                    it.send(DatagramPacket(body, body.size, InetAddress.getByName("255.255.255.255"), PORT))
                }
            }.onFailure { Log.w(TAG, "朋友圈广播失败", it) }
        }
    }

    private fun startMediaServer() {
        if (mediaServerJob?.isActive == true) return
        mediaServerJob = scope.launch {
            runCatching {
                ServerSocket().also {
                    mediaServer = it
                    it.reuseAddress = true
                    it.bind(InetSocketAddress(MEDIA_PORT))
                }.use { server ->
                    while (isActive) {
                        val client = server.accept()
                        launch {
                            client.use { socket ->
                                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                                val request = reader.readLine().orEmpty()
                                val encodedName = request.split(" ").getOrNull(1)
                                    ?.removePrefix("/media/")?.substringBefore("?")
                                val name = encodedName?.let {
                                    URLDecoder.decode(it, Charsets.UTF_8.name())
                                }
                                val file = name?.let(::findSharedMedia)
                                val output = socket.getOutputStream()
                                if (file == null) {
                                    output.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".encodeToByteArray())
                                } else {
                                    output.write(
                                        ("HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\n" +
                                            "Content-Length: ${file.length()}\r\nConnection: close\r\n\r\n")
                                            .encodeToByteArray()
                                    )
                                    file.inputStream().use { it.copyTo(output) }
                                }
                                output.flush()
                            }
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "朋友圈媒体服务已停止", it) }
        }
    }

    private fun findSharedMedia(name: String): File? {
        val safeName = File(name).name
        val state = repository.state.value
        val paths = buildList {
            addAll(state.covers.values.map { it.path })
            state.coverPath?.let(::add)
            state.moments.forEach { moment ->
                moment.authorAvatar?.let(::add)
                addAll(moment.images)
                moment.video?.let { video ->
                    add(video.path)
                    video.thumbnailPath?.let(::add)
                }
            }
        }
        return paths.asSequence()
            .filter { File(it).name == safeName }
            .map(::File)
            .firstOrNull { it.isFile }
    }

    private fun downloadRemoteMedia(state: MomentsState, host: String): MomentsState {
        val cache = mutableMapOf<String, String>()
        fun download(path: String?): String? {
            if (path == null) return null
            return cache.getOrPut(path) {
                runCatching {
                    val name = File(path).name
                    val directory = File(context.filesDir, "moments").apply { mkdirs() }
                    val target = File(directory, "remote_${host.replace(':', '_')}_$name")
                    if (target.isFile && target.length() > 0) return@runCatching target.absolutePath
                    val encoded = URLEncoder.encode(name, Charsets.UTF_8.name()).replace("+", "%20")
                    val connection = URL("http://$host:$MEDIA_PORT/media/$encoded")
                        .openConnection() as HttpURLConnection
                    connection.connectTimeout = MEDIA_TIMEOUT_MS
                    connection.readTimeout = MEDIA_TIMEOUT_MS
                    connection.inputStream.use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            var total = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= MAX_MEDIA_BYTES) { "朋友圈图片过大" }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    connection.disconnect()
                    target.absolutePath
                }.getOrElse {
                    Log.w(TAG, "下载朋友圈图片失败: $path", it)
                    path
                }
            }
        }
        return state.copy(
            covers = state.covers.mapValues { (_, cover) ->
                cover.copy(path = download(cover.path) ?: cover.path)
            },
            coverPath = download(state.coverPath),
            moments = state.moments.map { moment ->
                moment.copy(
                    authorAvatar = download(moment.authorAvatar),
                    images = moment.images.mapNotNull(::download),
                    video = moment.video?.let { video ->
                        video.copy(
                            path = download(video.path) ?: video.path,
                            thumbnailPath = download(video.thumbnailPath)
                        )
                    }
                )
            }
        )
    }

    private companion object {
        const val TAG = "MomentsLanSync"
        const val PORT = 38992
        const val MEDIA_PORT = 38993
        const val MAX_PACKET = 60 * 1024
        const val MEDIA_TIMEOUT_MS = 8_000
        const val MAX_MEDIA_BYTES = 20L * 1024 * 1024
    }
}
