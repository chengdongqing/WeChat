package top.chengdongqing.wechat.core.common.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import top.chengdongqing.wechat.core.designsystem.R as DesignR

/**
 * 提示音管理器
 */
@Singleton
class SoundTipPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    init {
        setupSoundPool()
    }

    private fun setupSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // 同时播放的最大数量
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION) // 告诉系统这是“交互反馈”或“辅助提示音”（类似按键音、扫码成功音）。系统会根据此标识自动处理：比如在通话中自动降低该声音音量，或在“免打扰”模式下遵循提示音的过滤规则。
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // 告诉系统这是“瞬态声”（短促的声音），而不是音乐（Music）或电影原声（Movie）。
                    .build()
            )
            .build()

        // 预加载常用音频
        preload(
            context,
            DesignR.raw.tip_call_end,
            DesignR.raw.ringtone_default,
            DesignR.raw.tip_voice_played,
            DesignR.raw.tip_qrcode_completed,
            DesignR.raw.tip_after_upload_voice
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
        soundMap[resId]?.let { id ->
            // 参数：soundId, 左音量, 右音量, 优先级, 循环, 速率
            soundPool?.play(id, 0.5f, 0.5f, 1, 0, 1f)
        }
    }
}

/**
 * 获取 Hilt 注入实例的入口点接口
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SoundPlayerEntryPoint {
    fun getSoundPlayer(): SoundTipPlayer
}

/**
 * 方便在 Composable 中复用的 remember 函数
 */
@Composable
fun rememberSoundTipPlayer(): SoundTipPlayer {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        EntryPointAccessors.fromApplication(
            context,
            SoundPlayerEntryPoint::class.java
        ).getSoundPlayer()
    }
}