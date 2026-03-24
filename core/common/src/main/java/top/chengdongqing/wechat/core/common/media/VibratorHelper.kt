package top.chengdongqing.wechat.core.common.media

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * 振动工具类
 */
@Singleton
class VibratorHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? = resolveVibrator()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * 按指定模式振动
     *
     * 系统关闭振动时不振动
     *
     * @param pattern 振动模式数组（毫秒）
     * @param repeat 重复索引（-1 表示不重复）
     */
    fun vibrate(pattern: LongArray, repeat: Int = -1) {
        if (!shouldVibrate()) return

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeat))
    }

    /**
     * 取消振动
     */
    fun cancel() {
        vibrator?.cancel()
    }

    /**
     * 获取 Vibrator 实例
     */
    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * 判断是否应该振动
     *
     * 非静音模式下才振动
     */
    private fun shouldVibrate() =
        audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
}