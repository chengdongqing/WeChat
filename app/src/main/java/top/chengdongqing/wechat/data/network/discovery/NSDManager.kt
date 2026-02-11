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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NSD (Network Service Discovery) 管理器
 * 用于 WiFi LAN 环境下的设备发现
 */
@Singleton
class NSDDiscoveryManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private companion object {
        const val TAG = "NSDDiscovery"
        const val SERVICE_TYPE = "_wechat._tcp."  // 自定义服务类型
        const val SERVICE_PORT = 8888             // 通信端口
    }

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * 注册服务（让其他设备发现我）
     */
    fun registerService(userId: String): Flow<ServiceRegistrationState> = callbackFlow {
        val serviceName = "WeChat_$userId"

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            serviceType = SERVICE_TYPE
            port = SERVICE_PORT

            // 添加额外属性
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
                Log.d(TAG, "服务已注册: ${serviceInfo.serviceName}")
                trySend(ServiceRegistrationState.Registered(serviceInfo.serviceName))
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务已注销")
                trySend(ServiceRegistrationState.Unregistered)
            }
        }

        registrationListener = listener

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "注册服务异常", e)
            trySend(ServiceRegistrationState.Failed(-1))
        }

        awaitClose {
            try {
                nsdManager.unregisterService(listener)
            } catch (e: Exception) {
                Log.e(TAG, "注销服务异常", e)
            }
            registrationListener = null
        }
    }

    /**
     * 发现服务（发现其他设备）
     */
    fun discoverServices(): Flow<DiscoveredDevice> = callbackFlow {
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

                // 解析服务详情
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                    nsdManager.registerServiceInfoCallback(
                        serviceInfo,
                        Runnable::run,
                        object : NsdManager.ServiceInfoCallback {
                            override fun onServiceUpdated(si: NsdServiceInfo) {
                                sendDevice(si)
                            }

                            override fun onServiceLost() {
                                Log.d(TAG, "回调通知：服务丢失")
                            }

                            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                                Log.e(TAG, "注册回调失败: $errorCode")
                            }

                            override fun onServiceInfoCallbackUnregistered() {}
                        })
                } else {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(si: NsdServiceInfo) {
                            sendDevice(si)
                        }
                    })
                }
            }

            // 内部复用发送逻辑
            private fun sendDevice(si: NsdServiceInfo) {
                val userId = si.attributes?.get("userId")?.let { String(it) }

                // 使用 getHostAddresses() 适配多 IP
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    si.hostAddresses.firstOrNull()?.hostAddress ?: ""
                } else {
                    @Suppress("DEPRECATION")
                    si.host?.hostAddress ?: ""
                }

                if (userId != null) {
                    trySend(
                        DiscoveredDevice(
                            userId = userId,
                            serviceName = si.serviceName,
                            host = address,
                            port = si.port
                        )
                    )
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务丢失: ${serviceInfo.serviceName}")
            }
        }

        discoveryListener = listener

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "发现服务异常", e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "停止发现异常", e)
            }
            discoveryListener = null
        }
    }
}

/**
 * 服务注册状态
 */
sealed class ServiceRegistrationState {
    data class Registered(val serviceName: String) : ServiceRegistrationState()
    data object Unregistered : ServiceRegistrationState()
    data class Failed(val errorCode: Int) : ServiceRegistrationState()
}

/**
 * 发现的设备
 */
data class DiscoveredDevice(
    val userId: String,
    val serviceName: String,
    val host: String,
    val port: Int
)