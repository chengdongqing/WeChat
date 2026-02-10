package top.chengdongqing.wechat.data.network.protocol

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.discovery.BLEDiscovery
import top.chengdongqing.wechat.data.network.service.P2PService

/**
 * P2P 消息传输器
 * 支持 JSON + 二进制数据的分阶段传输
 */
class P2PMessageTransmitter(
    private val bleDiscovery: BLEDiscovery,
    private val json: Json
) {

    companion object {
        private const val TAG = "P2PTransmitter"
    }

    /**
     * 发送消息（JSON + 可选的二进制数据）
     */
    @SuppressLint("MissingPermission")
    suspend fun sendMessage(
        targetUserId: String,
        message: P2PMessage,
        binaryData: ByteArray? = null
    ): Boolean {
        return try {
            // 1. 连接设备
            val gatt = bleDiscovery.scanAndConnect(targetUserId.toMD5Hex())
            if (gatt == null) {
                Log.e(TAG, "无法连接到设备: $targetUserId")
                return false
            }

            val characteristic = gatt.getService(
                P2PService.SERVICE_UUID
            )?.getCharacteristic(
                P2PService.CHARACTERISTIC_UUID
            )

            if (characteristic == null) {
                Log.e(TAG, "未找到特征")
                gatt.close()
                return false
            }

            // 2. 发送 JSON 消息
            val messageJson = json.encodeToString(message)
            val messageBytes = messageJson.toByteArray(Charsets.UTF_8)

            Log.d(TAG, "发送消息: ${message::class.simpleName}, JSON大小: ${messageBytes.size}")

            val jsonSuccess = bleDiscovery.writeCharacteristic(gatt, characteristic, messageBytes)

            if (!jsonSuccess) {
                Log.e(TAG, "JSON发送失败")
                gatt.close()
                return false
            }

            Log.d(TAG, "✅ JSON发送成功")

            // 3. 如果有二进制数据，继续发送
            if (binaryData != null) {
                delay(200)  // 短暂延迟

                Log.d(TAG, "发送二进制数据: ${binaryData.size} 字节")

                val binarySuccess =
                    bleDiscovery.writeCharacteristic(gatt, characteristic, binaryData)

                if (!binarySuccess) {
                    Log.e(TAG, "二进制数据发送失败")
                    gatt.close()
                    return false
                }

                Log.d(TAG, "✅ 二进制数据发送成功")
            }

            // 4. 关闭连接
            delay(500)
            gatt.close()

            true
        } catch (e: Exception) {
            Log.e(TAG, "发送消息失败", e)
            false
        }
    }
}

private fun String.toMD5Hex(): String {
    val md = java.security.MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}