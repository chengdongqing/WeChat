package top.chengdongqing.wechat.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import top.chengdongqing.wechat.R

/**
 * 提示音管理器
 */
object SoundTipPlayer {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    fun init(context: Context) {
        if (soundPool != null) return

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // 同时播放的最大数量
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        // 预加载常用音频
        preload(
            context,
            R.raw.sent_message,
            R.raw.playend,
            R.raw.phonering,
            R.raw.play_completed,
            R.raw.qrcode_completed,
            R.raw.after_upload_voice
        )
    }

    fun preload(context: Context, vararg resIds: Int) {
        resIds.forEach { resId ->
            if (!soundMap.containsKey(resId)) {
                soundPool?.load(context, resId, 1)?.let { id ->
                    soundMap[resId] = id
                }
            }
        }
    }

    fun play(@RawRes resId: Int) {
        val soundId = soundMap[resId]
        if (soundId != null) {
            // 参数：soundId, 左音量, 右音量, 优先级, 循环, 速率
            soundPool?.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }
}