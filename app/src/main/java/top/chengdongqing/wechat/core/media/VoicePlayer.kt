package top.chengdongqing.wechat.core.media

import android.media.MediaPlayer
import android.net.Uri

class VoicePlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(
        uri: Uri,
        onComplete: () -> Unit
    ) {
        stop()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(uri.path)
                setOnCompletionListener {
                    onComplete()
                    stop() // 播放完自动释放
                }
                setOnErrorListener { _, _, _ ->
                    onComplete() // 出错也要重置 UI 状态
                    stop()
                    true
                }
                prepareAsync()
                setOnPreparedListener {
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    fun stop() {
        mediaPlayer?.run {
            try {
                if (isPlaying) {
                    stop()
                }
            } catch (_: Exception) {
                // 某些状态下调用 stop 可能抛异常，直接忽略
            } finally {
                release()
            }
        }
        mediaPlayer = null
    }
}