package top.chengdongqing.wechat.data.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用于 WiFi LAN 环境下的设备发现
 */
@Singleton
class NSDDiscovery @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private companion object {
        const val TAG = "NSDDiscovery"
        const val SERVICE_TYPE = "_wechat._tcp."
        const val RESOLVE_RETRY_DELAY_MS = 500L
        const val RESOLVE_MAX_RETRY = 3
    }

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    // ==================== 注册服务 ====================

    /**
     * 注册服务（让其他设备发现我）
     */
    fun registerService(userId: String): Flow<ServiceRegistrationState> = callbackFlow {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "WeChat_$userId"
            serviceType = SERVICE_TYPE
            port = 0  // 系统自动分配
            setAttribute("userId", userId)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注册失败: $errorCode")
                trySend(ServiceRegistrationState.Failed(errorCode))
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注销失败: $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                // 注册成功后获取系统实际分配的端口
                Log.d(TAG, "✅ 服务已注册: ${serviceInfo.serviceName}, 端口: ${serviceInfo.port}")
                trySend(
                    ServiceRegistrationState.Registered(
                        serviceInfo.serviceName,
                        serviceInfo.port
                    )
                )
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务已注销")
                trySend(ServiceRegistrationState.Unregistered)
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "注册服务异常", e)
            trySend(ServiceRegistrationState.Failed(-1))
        }

        awaitClose {
            runCatching { nsdManager.unregisterService(listener) }
        }
    }

    // ==================== 发现服务 ====================

    /**
     * 发现其他设备
     * 返回 DiscoveryEvent（DeviceFound + DeviceLost）
     * 过滤自己
     * 串行解析队列防止 FAILURE_ALREADY_ACTIVE
     */
    fun discoverServices(myUserId: String): Flow<DiscoveryEvent> = callbackFlow {
        // 串行解析队列（仅旧版需要，Android 13+ 用新 API 不需要）
        val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
        val isResolving = AtomicBoolean(false)

        fun resolveNext() {
            if (isResolving.get()) return
            val next = resolveQueue.poll() ?: return
            isResolving.set(true)

            resolveWithRetry(
                serviceInfo = next,
                retryCount = 0,
                onResolved = { device ->
                    isResolving.set(false)
                    // 过滤自己
                    if (device.userId != myUserId) {
                        trySend(DiscoveryEvent.DeviceFound(device))
                    } else {
                        Log.d(TAG, "过滤自己: ${device.userId}")
                    }
                    resolveNext()
                },
                onFailed = {
                    isResolving.set(false)
                    resolveNext()
                }
            )
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "开始发现失败: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "停止发现失败: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "开始发现服务")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "停止发现服务")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "发现服务: ${serviceInfo.serviceName}")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+：使用新 API，不需要队列
                    nsdManager.registerServiceInfoCallback(
                        serviceInfo,
                        Runnable::run,
                        object : NsdManager.ServiceInfoCallback {
                            override fun onServiceUpdated(si: NsdServiceInfo) {
                                val device = si.toDiscoveredDevice() ?: return
                                if (device.userId != myUserId) {
                                    trySend(DiscoveryEvent.DeviceFound(device))
                                }
                            }

                            override fun onServiceLost() {
                                Log.d(TAG, "回调通知：服务丢失: ${serviceInfo.serviceName}")
                                trySend(DiscoveryEvent.DeviceLost(serviceInfo.serviceName))
                            }

                            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                                Log.e(TAG, "注册回调失败: $errorCode")
                            }

                            override fun onServiceInfoCallbackUnregistered() {
                                Log.d(TAG, "回调已注销: ${serviceInfo.serviceName}")
                            }
                        }
                    )
                } else {
                    // Android 13-：加入串行队列
                    resolveQueue.add(serviceInfo)
                    resolveNext()
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务丢失: ${serviceInfo.serviceName}")
                // Android 13- 的离线通知（14+ 在 ServiceInfoCallback.onServiceLost 里处理）
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    trySend(DiscoveryEvent.DeviceLost(serviceInfo.serviceName))
                }
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "发现服务异常", e)
        }

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * Android 13- 带重试的解析（避免 FAILURE_ALREADY_ACTIVE）
     */
    private fun resolveWithRetry(
        serviceInfo: NsdServiceInfo,
        retryCount: Int,
        onResolved: (DiscoveredDevice) -> Unit,
        onFailed: () -> Unit
    ) {
        @Suppress("DEPRECATION")
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "解析失败: errorCode=$errorCode, retry=$retryCount")

                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE
                    && retryCount < RESOLVE_MAX_RETRY
                ) {
                    // 延迟重试，每次递增延迟
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        resolveWithRetry(si, retryCount + 1, onResolved, onFailed)
                    }, RESOLVE_RETRY_DELAY_MS * (retryCount + 1))
                } else {
                    Log.e(TAG, "解析最终失败: ${si.serviceName}")
                    onFailed()
                }
            }

            override fun onServiceResolved(si: NsdServiceInfo) {
                val device = si.toDiscoveredDevice()
                if (device != null) {
                    Log.d(TAG, "✅ 解析成功: ${device.userId} @ ${device.host}:${device.port}")
                    onResolved(device)
                } else {
                    Log.w(TAG, "解析成功但数据无效: ${si.serviceName}")
                    onFailed()
                }
            }
        })
    }

    /**
     * NsdServiceInfo → DiscoveredDevice
     * 优先取内网 IPv4，过滤回环地址
     */
    private fun NsdServiceInfo.toDiscoveredDevice(): DiscoveredDevice? {
        val userId = attributes?.get("userId")?.let { String(it) }
            ?: return null

        val host = resolveHost()
            ?: return null

        return DiscoveredDevice(
            userId = userId,
            serviceName = serviceName,
            host = host,
            port = port
        )
    }

    /**
     * 优先取内网 IPv4，过滤回环/IPv6
     */
    private fun NsdServiceInfo.resolveHost(): String? {
        val addresses: List<InetAddress> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                hostAddresses
            } else {
                @Suppress("DEPRECATION")
                listOfNotNull(host)
            }

        return addresses
            .filter { !it.isLoopbackAddress }           // 过滤回环
            .filterIsInstance<Inet4Address>()      // 优先 IPv4
            .firstOrNull { it.isSiteLocalAddress }       // 优先内网地址
            ?.hostAddress
            ?: addresses.firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress                            // 兜底：任意非回环地址
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