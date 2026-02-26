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
import top.chengdongqing.wechat.data.network.discovery.NSDDiscovery.Companion.RESOLVE_MAX_RETRIES
import java.net.Inet4Address
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NSD（Network Service Discovery）局域网设备发现
 *
 * 基于 mDNS/DNS-SD，在 WiFi LAN 内注册和发现设备。
 *
 * Android 版本差异：
 * - Android 14+：[NsdManager.registerServiceInfoCallback] 直接订阅服务变化，无并发限制
 * - Android 13-：[NsdManager.resolveService] 不支持并发，由 [SerialServiceResolver] 串行处理
 *
 * NSD 缓存问题：onServiceFound 里的 serviceInfo.port 可能是缓存旧值，
 * 必须通过 resolve 或 ServiceInfoCallback 拿最新的 host 和 port。
 */
@Singleton
class NSDDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "NSDDiscovery"
        const val SERVICE_TYPE = "_wechat._tcp."
        const val SERVICE_NAME_PREFIX = "WeChat_"
        const val ATTR_KEY_USER_ID = "userId"
        const val RESOLVE_RETRY_DELAY_MS = 500L
        const val RESOLVE_MAX_RETRIES = 3
    }

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * 注册本地服务，使局域网内其他设备能发现本机
     *
     * 注：[NsdManager.RegistrationListener.onServiceRegistered] 回调里的 port 在部分设备上
     * 会返回 0，[ServiceRegistrationState.Registered.port] 已改为使用传入的 [localPort]。
     *
     * serviceName 加时间戳后缀，避免系统 mDNS 层残留旧注册导致回调静默丢失。
     */
    fun registerService(userId: String, localPort: Int): Flow<ServiceRegistrationState> =
        callbackFlow {
            if (localPort <= 0) {
                Log.e(TAG, "无效端口: $localPort")
                trySend(ServiceRegistrationState.Failed(-1))
                close()
                return@callbackFlow
            }

            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "${SERVICE_NAME_PREFIX}${userId}_${System.currentTimeMillis()}"
                serviceType = SERVICE_TYPE
                port = localPort
                setAttribute(ATTR_KEY_USER_ID, userId)
            }
            val listener = createRegistrationListener(localPort)

            runCatching {
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            }.onFailure {
                Log.e(TAG, "注册服务异常", it)
                trySend(ServiceRegistrationState.Failed(-1))
            }

            awaitClose {
                runCatching { nsdManager.unregisterService(listener) }
            }
        }

    private fun ProducerScope<ServiceRegistrationState>.createRegistrationListener(localPort: Int) =
        object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                trySend(ServiceRegistrationState.Registered(info.serviceName, localPort))
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注册失败: errorCode=$errorCode name=${info.serviceName}")
                trySend(ServiceRegistrationState.Failed(errorCode))
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                trySend(ServiceRegistrationState.Unregistered)
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注销失败: errorCode=$errorCode")
            }
        }

    /**
     * 发现局域网内其他用户，持续监听直到 Flow 被取消
     *
     * @param currentUserId 当前用户 ID，用于过滤自身服务
     */
    fun discoverServices(currentUserId: String): Flow<DiscoveryEvent> = callbackFlow {
        val serialResolver = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SerialServiceResolver(this, currentUserId)
        } else null

        val listener = createDiscoveryListener(currentUserId, serialResolver)

        runCatching {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            Log.e(TAG, "启动服务发现失败", it)
        }

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(listener) }
            Log.d(TAG, "服务发现已停止")
        }
    }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerServiceInfoCallback(serviceInfo, currentUserId)
            } else {
                serialResolver?.enqueue(serviceInfo)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            // Android 14+ 由 ServiceInfoCallback.onServiceLost 通知，此处只处理 13-
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                trySend(DiscoveryEvent.DeviceLost(serviceInfo.serviceName))
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "启动发现失败: errorCode=$errorCode type=$serviceType")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "停止发现失败: errorCode=$errorCode")
        }
    }

    /**
     * 订阅服务信息实时回调（Android 14+）
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
                    if (shouldNotify(device, currentUserId)) {
                        trySend(DiscoveryEvent.DeviceFound(device))
                    }
                }

                override fun onServiceLost() {
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
     * 串行服务解析器
     *
     * Android 13- 的 [NsdManager.resolveService] 同一时刻只能有一个解析，
     * 并发调用返回 [NsdManager.FAILURE_ALREADY_ACTIVE]。
     * 通过队列串行处理，每次完成后自动取下一个。
     */
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
            val serviceInfo = queue.poll() ?: run { isProcessing.set(false); return }

            resolveWithRetry(
                serviceInfo = serviceInfo,
                retryCount = 0,
                onResolved = { device ->
                    if (shouldNotify(device, currentUserId)) {
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

    /**
     * 带退避重试的服务解析（Android 13-）
     *
     * [NsdManager.FAILURE_ALREADY_ACTIVE] 时按线性退避重试，最多 [RESOLVE_MAX_RETRIES] 次。
     */
    private fun resolveWithRetry(
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
                    Log.d(TAG, "解析成功: ${device.userId} @ ${device.host}:${device.port}")
                    onResolved(device)
                } else {
                    Log.w(TAG, "解析成功但数据无效: ${resolvedInfo.serviceName}")
                    onFailed()
                }
            }

            override fun onResolveFailed(failedInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(
                    TAG,
                    "解析失败: errorCode=$errorCode retry=$retryCount name=${failedInfo.serviceName}"
                )
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE && retryCount < RESOLVE_MAX_RETRIES) {
                    mainHandler.postDelayed(
                        { resolveWithRetry(serviceInfo, retryCount + 1, onResolved, onFailed) },
                        RESOLVE_RETRY_DELAY_MS * (retryCount + 1)
                    )
                } else {
                    Log.e(TAG, "解析最终失败: ${failedInfo.serviceName}")
                    onFailed()
                }
            }
        })
    }

    private fun shouldNotify(device: DiscoveredDevice, currentUserId: String): Boolean {
        return device.userId != currentUserId
    }

    private fun NsdServiceInfo.toDiscoveredDevice(): DiscoveredDevice? {
        val userId = attributes?.get(ATTR_KEY_USER_ID)?.let { String(it) } ?: return null
        val host = extractHost() ?: return null
        return DiscoveredDevice(
            userId = userId,
            serviceName = serviceName,
            host = host,
            port = port
        )
    }

    /**
     * 提取有效主机地址
     *
     * 过滤掉回环地址（127.x / ::1）后按优先级选取：
     * 1. 局域网 IPv4（192.168.x / 10.x / 172.16-31.x）— LAN 直连首选
     * 2. 任意 IPv4 — 兜底，覆盖非标准内网段
     * 3. 任意非回环地址 — 最终兜底，IPv6 或其他
     */
    @SuppressLint("NewApi")
    private fun NsdServiceInfo.extractHost(): String? {
        // 过滤回环地址，剩余为候选地址
        val valid = hostAddresses.filter { !it.isLoopbackAddress }
        if (valid.isEmpty()) return null

        val ipv4 = valid.filterIsInstance<Inet4Address>()
        return (ipv4.firstOrNull { it.isSiteLocalAddress }  // 优先局域网 IPv4
            ?: ipv4.firstOrNull()                           // 次选任意 IPv4
            ?: valid.firstOrNull())                         // 兜底任意非回环地址
            ?.hostAddress
    }
}

sealed class DiscoveryEvent {
    data class DeviceFound(val device: DiscoveredDevice) : DiscoveryEvent()
    data class DeviceLost(val serviceName: String) : DiscoveryEvent()
}

sealed class ServiceRegistrationState {
    /** 注册成功，[port] 为实际传入的监听端口（非系统回调值） */
    data class Registered(val serviceName: String, val port: Int) : ServiceRegistrationState()
    data object Unregistered : ServiceRegistrationState()
    data class Failed(val errorCode: Int) : ServiceRegistrationState()
}

data class DiscoveredDevice(
    val userId: String,
    val serviceName: String,
    val host: String,
    val port: Int
)