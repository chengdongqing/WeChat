package top.chengdongqing.wechat.data.network.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiFi & CPU 锁管理器
 *
 * Android 在屏幕关闭后会逐步降低 WiFi 功率甚至断开连接，
 * 大文件后台传输会因此速度骤降或中断。
 *
 * 本管理器通过引用计数维护锁的生命周期:
 * - 第一个传输任务 acquire → 获取锁
 * - 最后一个传输任务 release → 释放锁
 * - 中间有多个并发传输时，锁保持持有状态
 *
 * 需要权限: android.permission.WAKE_LOCK
 */
@Singleton
class WifiLockManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "WifiLockManager"
        const val WIFI_LOCK_TAG = "WeChat:FileTransfer"
        const val WAKE_LOCK_TAG = "WeChat:FileTransfer"
    }

    private val refCount = AtomicInteger(0)

    /**
     * WiFi High Performance Lock
     *
     * WIFI_MODE_FULL_HIGH_PERF:
     * - 保持 WiFi 连接活跃，不降功率
     * - 禁止 WiFi 省电模式（PSM），避免延迟波动
     * - 屏幕关闭后仍维持高性能模式
     */
    @Suppress("DEPRECATION")
    private val wifiLock: WifiManager.WifiLock by lazy {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            WIFI_LOCK_TAG
        )
    }

    /**
     * Partial Wake Lock
     *
     * 保持 CPU 运行，防止系统在传输过程中休眠。
     * 仅保持 CPU，不保持屏幕亮度（对用户无感知）。
     */
    private val wakeLock: PowerManager.WakeLock by lazy {
        val powerManager = context.applicationContext
            .getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )
    }

    /**
     * 获取传输锁
     *
     * 引用计数 0→1 时真正获取锁，后续调用仅增加计数。
     * 必须与 [release] 配对调用，建议用 [withLock] 代替。
     */
    fun acquire() {
        if (refCount.incrementAndGet() == 1) {
            if (!wifiLock.isHeld) {
                wifiLock.acquire()
                Log.d(TAG, "WiFi lock acquired")
            }
            if (!wakeLock.isHeld) {
                wakeLock.acquire(20 * 60 * 1000L /*20 minutes*/)
                Log.d(TAG, "Wake lock acquired")
            }
        }
    }

    /**
     * 释放传输锁
     *
     * 引用计数归零时真正释放锁。
     */
    fun release() {
        if (refCount.decrementAndGet() <= 0) {
            refCount.set(0) // 防止负数
            if (wifiLock.isHeld) {
                wifiLock.release()
                Log.d(TAG, "WiFi lock released")
            }
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "Wake lock released")
            }
        }
    }

    /**
     * 在 block 执行期间持有锁，结束后自动释放
     */
    inline fun <T> withLock(block: () -> T): T {
        acquire()
        try {
            return block()
        } finally {
            release()
        }
    }
}