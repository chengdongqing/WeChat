package top.chengdongqing.wechat.features.call.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 声音播放器
 *
 * 负责播放通话相关的提示音
 */
@Singleton
class SoundPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * 声音类型
     */
    enum class Sound(@get:RawRes val rawResId: Int) {
        /** 连接中提示音 */
        Connecting(R.raw.phonering),

        /** 来电铃声 */
        Ringing(R.raw.phonering),

        /** 通话结束音 */
        CallEnd(R.raw.playend)
    }

    private val soundPool: SoundPool by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private val soundIds = mutableMapOf<Sound, Int>()
    private var currentStreamId: Int? = null

    init {
        loadSounds()
    }

    /**
     * 预加载所有音效
     */
    private fun loadSounds() {
        Sound.entries.forEach { sound ->
            soundIds[sound] = soundPool.load(context, sound.rawResId, 1)
        }
    }

    /**
     * 播放指定音效
     */
    fun play(sound: Sound) {
        soundIds[sound]?.let { soundId ->
            stop() // 停止当前播放
            currentStreamId = soundPool.play(
                soundId,
                1f, // 左声道音量
                1f, // 右声道音量
                1,  // 优先级
                0,  // 循环次数（0表示不循环）
                1f  // 播放速率
            )
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        currentStreamId?.let { streamId ->
            soundPool.stop(streamId)
            currentStreamId = null
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        stop()
        soundPool.release()
        soundIds.clear()
    }
}