package top.chengdongqing.wechat.data.network.discovery

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NSD (Network Service Discovery) 服务发现
 *
 * 用于 WiFi LAN 环境下的设备发现,支持:
 * - 注册本地服务供其他设备发现
 * - 发现局域网内的其他设备
 * - Android 14+ 使用新 API,13- 使用串行解析队列防止并发冲突
 */
@Singleton
class NSDDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private companion object {
        const val TAG = "NSDDiscovery"

        // 服务类型定义
        const val SERVICE_TYPE = "_wechat._tcp."
        const val SERVICE_NAME_PREFIX = "WeChat_"
        const val ATTR_KEY_USER_ID = "userId"

        // 重试配置
        const val RESOLVE_RETRY_DELAY_MS = 500L
        const val RESOLVE_MAX_RETRIES = 3
    }

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    // ==================== 服务注册 ====================

    /**
     * 注册本地服务,使其他设备能够发现本机
     *
     * @param userId 用户 ID,用于标识和过滤
     * @param localPort 本地监听端口
     * @return 注册状态流
     */
    fun registerService(userId: String, localPort: Int): Flow<ServiceRegistrationState> =
        callbackFlow {
            // 验证端口有效性
            if (localPort <= 0) {
                Log.e(TAG, "无效端口: $localPort")
                trySend(ServiceRegistrationState.Failed(-1))
                close()
                return@callbackFlow
            }

            val serviceInfo = buildServiceInfo(userId, localPort)
            val listener = createRegistrationListener()

            try {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Log.e(TAG, "注册服务异常", e)
                trySend(ServiceRegistrationState.Failed(-1))
            }

            awaitClose {
                runCatching {
                    nsdManager.unregisterService(listener)
                    Log.d(TAG, "服务已注销")
                }
            }
        }

    /**
     * 构建服务信息
     */
    private fun buildServiceInfo(userId: String, localPort: Int): NsdServiceInfo {
        return NsdServiceInfo().apply {
            serviceName = "$SERVICE_NAME_PREFIX$userId"
            serviceType = SERVICE_TYPE
            port = localPort
            setAttribute(ATTR_KEY_USER_ID, userId)
        }
    }

    /**
     * 创建注册监听器
     */
    private fun ProducerScope<ServiceRegistrationState>.createRegistrationListener() =
        object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "✅ 服务注册成功: ${serviceInfo.serviceName}, 端口: ${serviceInfo.port}")
                trySend(
                    ServiceRegistrationState.Registered(
                        serviceName = serviceInfo.serviceName,
                        port = serviceInfo.port
                    )
                )
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注册失败: errorCode=$errorCode, service=${serviceInfo.serviceName}")
                trySend(ServiceRegistrationState.Failed(errorCode))
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务注销成功: ${serviceInfo.serviceName}")
                trySend(ServiceRegistrationState.Unregistered)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注销失败: errorCode=$errorCode")
            }
        }

    // ==================== 服务发现 ====================

    /**
     * 发现局域网内的其他设备
     *
     * @param currentUserId 当前用户 ID,用于过滤自己
     * @return 设备发现/丢失事件流
     *
     * 注意:
     * - Android 14+ 使用新 API 直接获取设备信息
     * - Android 13- 使用串行队列防止 FAILURE_ALREADY_ACTIVE 错误
     */
    fun discoverServices(currentUserId: String): Flow<DiscoveryEvent> = callbackFlow {
        val serialResolver = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            createSerialResolver(currentUserId)
        } else {
            null
        }

        val listener = createDiscoveryListener(currentUserId, serialResolver)

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "启动服务发现失败", e)
        }

        awaitClose {
            runCatching {
                nsdManager.stopServiceDiscovery(listener)
                Log.d(TAG, "服务发现已停止")
            }
        }
    }

    /**
     * 创建服务发现监听器
     */
    private fun ProducerScope<DiscoveryEvent>.createDiscoveryListener(
        currentUserId: String,
        serialResolver: SerialServiceResolver?
    ) = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(serviceType: String) {
            Log.d(TAG, "服务发现已启动: $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "服务发现已停止: $serviceType")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "发现服务: ${serviceInfo.serviceName}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+: 使用新 API 直接注册回调
                registerServiceInfoCallback(serviceInfo, currentUserId)
            } else {
                // Android 13-: 加入串行解析队列
                serialResolver?.enqueue(serviceInfo)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "服务丢失: ${serviceInfo.serviceName}")
            // Android 13- 通过此回调通知,14+ 通过 ServiceInfoCallback 通知
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                trySend(DiscoveryEvent.DeviceLost(serviceInfo.serviceName))
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "启动发现失败: errorCode=$errorCode, type=$serviceType")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "停止发现失败: errorCode=$errorCode")
        }
    }

    /**
     * 注册服务信息回调 (Android 14+)
     */
    @Suppress("NewApi")
    private fun ProducerScope<DiscoveryEvent>.registerServiceInfoCallback(
        serviceInfo: NsdServiceInfo,
        currentUserId: String
    ) {
        nsdManager.registerServiceInfoCallback(
            serviceInfo,
            Runnable::run,
            object : NsdManager.ServiceInfoCallback {
                override fun onServiceUpdated(resolvedInfo: NsdServiceInfo) {
                    val device = resolvedInfo.toDiscoveredDevice() ?: return
                    if (shouldNotifyDevice(device, currentUserId)) {
                        trySend(DiscoveryEvent.DeviceFound(device))
                    }
                }

                override fun onServiceLost() {
                    Log.d(TAG, "服务丢失(回调): ${serviceInfo.serviceName}")
                    trySend(DiscoveryEvent.DeviceLost(serviceInfo.serviceName))
                }

                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                    Log.e(TAG, "回调注册失败: errorCode=$errorCode")
                }

                override fun onServiceInfoCallbackUnregistered() {
                    Log.d(TAG, "回调已注销: ${serviceInfo.serviceName}")
                }
            }
        )
    }

    /**
     * 串行服务解析器 (Android 13-)
     *
     * 用于避免同时调用多个 resolveService 导致的 FAILURE_ALREADY_ACTIVE 错误
     */
    private fun ProducerScope<DiscoveryEvent>.createSerialResolver(
        currentUserId: String
    ): SerialServiceResolver {
        return SerialServiceResolver(this, currentUserId)
    }

    private inner class SerialServiceResolver(
        private val scope: ProducerScope<DiscoveryEvent>,
        private val currentUserId: String
    ) {
        private val queue = ConcurrentLinkedQueue<NsdServiceInfo>()
        private val isProcessing = AtomicBoolean(false)

        fun enqueue(serviceInfo: NsdServiceInfo) {
            queue.offer(serviceInfo)
            processNext()
        }

        private fun processNext() {
            if (!isProcessing.compareAndSet(false, true)) return

            val serviceInfo = queue.poll()
            if (serviceInfo == null) {
                isProcessing.set(false)
                return
            }

            resolveServiceWithRetry(
                serviceInfo = serviceInfo,
                retryCount = 0,
                onResolved = { device ->
                    if (shouldNotifyDevice(device, currentUserId)) {
                        scope.trySend(DiscoveryEvent.DeviceFound(device))
                    }
                    isProcessing.set(false)
                    processNext()
                },
                onFailed = {
                    isProcessing.set(false)
                    processNext()
                }
            )
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断是否应该通知该设备
     */
    private fun shouldNotifyDevice(device: DiscoveredDevice, currentUserId: String): Boolean {
        return if (device.userId == currentUserId) {
            Log.d(TAG, "已过滤自己: ${device.userId}")
            false
        } else {
            Log.d(TAG, "✅ 发现设备: ${device.userId} @ ${device.host}:${device.port}")
            true
        }
    }

    /**
     * 带重试的服务解析 (Android 13-)
     *
     * @param serviceInfo 待解析的服务信息
     * @param retryCount 当前重试次数
     * @param onResolved 解析成功回调
     * @param onFailed 解析失败回调
     */
    private fun resolveServiceWithRetry(
        serviceInfo: NsdServiceInfo,
        retryCount: Int,
        onResolved: (DiscoveredDevice) -> Unit,
        onFailed: () -> Unit
    ) {
        @Suppress("DEPRECATION")
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val device = resolvedInfo.toDiscoveredDevice()
                if (device != null) {
                    Log.d(TAG, "✅ 解析成功: ${device.userId} @ ${device.host}:${device.port}")
                    onResolved(device)
                } else {
                    Log.w(TAG, "解析成功但数据无效: ${resolvedInfo.serviceName}")
                    onFailed()
                }
            }

            override fun onResolveFailed(failedInfo: NsdServiceInfo, errorCode: Int) {
                handleResolveFailed(failedInfo, errorCode, retryCount, onResolved, onFailed)
            }
        })
    }

    /**
     * 处理解析失败
     */
    private fun handleResolveFailed(
        serviceInfo: NsdServiceInfo,
        errorCode: Int,
        retryCount: Int,
        onResolved: (DiscoveredDevice) -> Unit,
        onFailed: () -> Unit
    ) {
        Log.w(
            TAG,
            "解析失败: errorCode=$errorCode, retry=$retryCount, service=${serviceInfo.serviceName}"
        )

        val shouldRetry = errorCode == NsdManager.FAILURE_ALREADY_ACTIVE
                && retryCount < RESOLVE_MAX_RETRIES

        if (shouldRetry) {
            scheduleRetry(serviceInfo, retryCount, onResolved, onFailed)
        } else {
            Log.e(TAG, "解析最终失败: ${serviceInfo.serviceName}")
            onFailed()
        }
    }

    /**
     * 调度重试
     */
    private fun scheduleRetry(
        serviceInfo: NsdServiceInfo,
        retryCount: Int,
        onResolved: (DiscoveredDevice) -> Unit,
        onFailed: () -> Unit
    ) {
        val delayMs = RESOLVE_RETRY_DELAY_MS * (retryCount + 1)
        mainHandler.postDelayed({
            resolveServiceWithRetry(serviceInfo, retryCount + 1, onResolved, onFailed)
        }, delayMs)
    }

    /**
     * 将 NsdServiceInfo 转换为 DiscoveredDevice
     */
    private fun NsdServiceInfo.toDiscoveredDevice(): DiscoveredDevice? {
        val userId = extractUserId() ?: return null
        val host = extractHost() ?: return null

        return DiscoveredDevice(
            userId = userId,
            serviceName = serviceName,
            host = host,
            port = port
        )
    }

    /**
     * 提取用户 ID
     */
    private fun NsdServiceInfo.extractUserId(): String? {
        return attributes?.get(ATTR_KEY_USER_ID)?.let { String(it) }
    }

    /**
     * 提取主机地址
     *
     * 优先级:
     * 1. 内网 IPv4 地址
     * 2. 任意 IPv4 地址
     * 3. 任意非回环地址
     */
    @SuppressLint("NewApi")
    private fun NsdServiceInfo.extractHost(): String? {
        val addresses = hostAddresses
        if (addresses.isEmpty()) return null

        // 过滤回环地址
        val validAddresses = addresses.filter { !it.isLoopbackAddress }
        if (validAddresses.isEmpty()) return null

        // 优先内网 IPv4
        val ipv4Addresses = validAddresses.filterIsInstance<Inet4Address>()
        val siteLocalAddress = ipv4Addresses.firstOrNull { it.isSiteLocalAddress }
        if (siteLocalAddress != null) return siteLocalAddress.hostAddress

        // 其次任意 IPv4
        val anyIpv4 = ipv4Addresses.firstOrNull()
        if (anyIpv4 != null) return anyIpv4.hostAddress

        // 兜底: 任意有效地址
        return validAddresses.firstOrNull()?.hostAddress
    }
}

// ==================== 数据类 ====================

sealed class DiscoveryEvent {
    data class DeviceFound(val device: DiscoveredDevice) : DiscoveryEvent()
    data class DeviceLost(val serviceName: String) : DiscoveryEvent()
}

sealed class ServiceRegistrationState {
    // 携带实际分配的端口
    data class Registered(
        val serviceName: String,
        val port: Int
    ) : ServiceRegistrationState()

    data object Unregistered : ServiceRegistrationState()
    data class Failed(val errorCode: Int) : ServiceRegistrationState()
}

data class DiscoveredDevice(
    val userId: String,
    val serviceName: String,
    val host: String,
    val port: Int
)