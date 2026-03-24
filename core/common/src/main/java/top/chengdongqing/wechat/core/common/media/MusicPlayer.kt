package top.chengdongqing.wechat.core.common.media

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 封装 MediaPlayer 生命周期
 */
class MusicPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    /** 当前播放进度 0f~1f */
    var progress by mutableFloatStateOf(0f)
        private set

    /** 是否正在播放 */
    var isPlaying by mutableStateOf(false)
        private set

    /** 总时长（毫秒），prepare 完成后才有值 */
    var duration by mutableIntStateOf(0)
        private set

    /**
     * 加载并准备音频资源
     */
    fun prepare(@RawRes resId: Int) {
        release()
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            this@MusicPlayer.duration = duration
            // 播放完毕后重置状态
            setOnCompletionListener {
                this@MusicPlayer.isPlaying = false
                this@MusicPlayer.progress = 0f
            }
        }
    }

    fun play() {
        mediaPlayer?.start()
        isPlaying = true
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun togglePlay() {
        if (isPlaying) pause() else play()
    }

    /**
     * 跳转到指定进度
     * @param fraction 0f~1f
     */
    fun seekTo(fraction: Float) {
        mediaPlayer?.let {
            it.seekTo((fraction * it.duration).toInt())
            progress = fraction
        }
    }

    /**
     * 刷新当前播放进度
     */
    fun updateProgress() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying && mp.duration > 0) {
                progress = mp.currentPosition.toFloat() / mp.duration
            }
        }
    }

    /**
     * 释放 MediaPlayer 资源
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        progress = 0f
    }
}