package top.chengdongqing.wechat.data.network.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import top.chengdongqing.wechat.data.network.transfer.WiFiLockManager.Companion.TRANSFER_WAKE_LOCK_TIMEOUT_MS
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiFi 锁和 CPU 唤醒锁管理器
 *
 * 两种锁各自维护引用计数，支持嵌套调用：
 * - KeepAlive（WiFi 锁）：应用运行期间持有，防止 WiFi 省电模式断连后台 Socket
 * - Transfer（WakeLock）：文件传输期间额外持有，防止 CPU 休眠导致传输中断
 *
 * [withTransferLock] 内部会自动 acquire/release KeepAlive，调用方无需手动管理。
 */
@Singleton
class WiFiLockManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "WiFiLockManager"
        const val TRANSFER_WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L  // 最长持锁 30 分钟
    }

    private val wifiRefCount = AtomicInteger(0)
    private val wakeRefCount = AtomicInteger(0)

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * WiFi 锁
     *
     * Android 10+：WIFI_MODE_FULL_LOW_LATENCY，低延迟模式，适合实时通信
     * Android 9-：WIFI_MODE_FULL_HIGH_PERF，高性能模式
     */
    private val wifiLock: WifiManager.WifiLock by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                "Chat:LowLatencyLock"
            )
        } else {
            @Suppress("DEPRECATION")
            wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Chat:WifiLock")
        }
    }

    /**
     * CPU 唤醒锁
     *
     * 文件传输时持有，防止 CPU 休眠后 Socket 写入被挂起。
     * WiFi 未断但 CPU 睡了同样会导致传输中断。
     */
    private val wakeLock: PowerManager.WakeLock by lazy {
        (context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Chat:WakeLock")
    }

    /** 获取 WiFi 活跃锁，引用计数为 1 时实际加锁 */
    fun acquireKeepAlive() {
        if (wifiRefCount.incrementAndGet() == 1 && !wifiLock.isHeld) {
            wifiLock.acquire()
            Log.d(TAG, "WiFi 锁已获取: ${wifiLockModeName()}")
        }
    }

    /** 释放 WiFi 活跃锁，引用计数归零时实际释放 */
    fun releaseKeepAlive() {
        if (wifiRefCount.decrementAndGet() <= 0) {
            wifiRefCount.set(0)
            if (wifiLock.isHeld) {
                wifiLock.release()
                Log.d(TAG, "WiFi 锁已释放")
            }
        }
    }

    /**
     * 在文件传输锁保护下执行 [block]
     *
     * 自动持有 WiFi 锁和 CPU 唤醒锁，block 结束后统一释放。
     * 唤醒锁最长持有 [TRANSFER_WAKE_LOCK_TIMEOUT_MS]，防止异常情况下永久持锁。
     */
    suspend fun <T> withTransferLock(block: suspend () -> T): T {
        acquireKeepAlive()
        if (wakeRefCount.incrementAndGet() == 1 && !wakeLock.isHeld) {
            wakeLock.acquire(TRANSFER_WAKE_LOCK_TIMEOUT_MS)
        }
        try {
            return block()
        } finally {
            if (wakeRefCount.decrementAndGet() <= 0) {
                wakeRefCount.set(0)
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
            releaseKeepAlive()
        }
    }

    private fun wifiLockModeName() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "LOW_LATENCY"
    } else {
        "HIGH_PERF"
    }
}