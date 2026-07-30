package top.chengdongqing.wechat.core.network.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.data.model.FriendProtocol
import top.chengdongqing.wechat.core.model.ProfileBeacon
import top.chengdongqing.wechat.core.util.toMD5Hex
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages outbound BLE client connections (central role).
 *
 * Each public method is a self-contained operation:
 *   scan → connect → send/receive → close
 */
@Singleton
class BLEConnectionManager @Inject constructor(
    private val bleClient: BLEClient,
    private val json: Json,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope,
) {
    companion object {
        const val TAG = "BLEConnectionManager"
    }

    /**
     * Sends a [FriendProtocol] message (with optional [binary] payload) to [targetUserId].
     *
     * Wire sequence: JSON chunks → BINARY chunks (optional) → END
     *
     * @return true if all packets were acknowledged by the remote device.
     */
    suspend fun sendMessage(
        targetUserId: String,
        message: FriendProtocol,
        binary: ByteArray? = null,
    ): Result<Unit> {
        val conn = openConnection(targetUserId) ?: return Result.failure(Exception("设备连接失败"))

        return runCatching {
            val jsonBytes = json.encodeToString<FriendProtocol>(message).toByteArray(Charsets.UTF_8)
            if (!conn.sendChunked(BLEPacketType.JSON, jsonBytes)) {
                throw Exception("发送失败")
            }

            if (binary != null) {
                // Brief pause between phases to allow the remote side to process the JSON
                delay(BLEConfig.CLOSE_DELAY_MS / 4)
                if (!conn.sendChunked(BLEPacketType.BINARY, binary)) {
                    throw Exception("发送失败")
                }
            }

            if (!conn.sendPacket(BLEPacket.end())) {
                throw Exception("发送失败")
            }
        }.also {
            delay(BLEConfig.CLOSE_DELAY_MS)
            conn.close()
        }
    }

    /**
     * Scans for [targetUserIdHash], connects, subscribes to notifications,
     * and reassembles the remote device's [ProfileBeacon] + avatar bytes.
     *
     * @param targetUserIdHash full MD5 hex string of the target userId
     * @return a pair of (ProfileBeacon, avatarBytes?), or null on failure / timeout.
     */
    suspend fun readProfile(targetUserIdHash: String): Result<Pair<ProfileBeacon, ByteArray?>> {
        val device = bleClient.scanForDevice(targetUserIdHash) ?: run {
            return Result.failure(Exception("未找到目标设备"))
        }
        val conn = openConnectionToDevice(device) ?: run {
            return Result.failure(Exception("设备连接失败"))
        }

        return runCatching {
            if (!conn.subscribeToNotifications()) {
                throw Exception("订阅 Notification 失败")
            }
            withTimeoutOrNull(BLEConfig.READ_TIMEOUT_MS) {
                conn.receiveProfileBeacon(json)
            } ?: run {
                throw Exception("读取资料超时")
            }
        }.also {
            conn.close()
        }
    }

    private suspend fun openConnection(targetUserId: String): BLEConnection? {
        val device = bleClient.scanForDevice(targetUserId.toMD5Hex()) ?: run {
            Log.w(TAG, "未找到设备: $targetUserId")
            return null
        }
        return openConnectionToDevice(device)
    }

    private suspend fun openConnectionToDevice(device: BluetoothDevice): BLEConnection? {
        val conn = BLEConnection(context, scope)
        return if (conn.connect(device)) {
            conn
        } else {
            Log.w(TAG, "连接失败: ${device.address}")
            conn.close()
            null
        }
    }
}

/**
 * Splits [data] into [BLEConfig.MAX_PACKET_BODY]-byte chunks, wraps each in a [BLEPacket]
 * of [type], and sends them sequentially via [BLEConnection.sendPacket].
 *
 * Each send suspends until the remote device acknowledges to write, providing natural
 * back-pressure without needing an explicit inter-packet delay.
 *
 * @return false if any individual write fails.
 */
private suspend fun BLEConnection.sendChunked(type: Byte, data: ByteArray): Boolean {
    var offset = 0
    var chunkIndex = 0
    while (offset < data.size) {
        val end = minOf(offset + BLEConfig.MAX_PACKET_BODY, data.size)
        val packet = BLEPacket(type, data.copyOfRange(offset, end))
        if (!sendPacket(packet)) {
            Log.e(BLEConnectionManager.TAG, "片段 #$chunkIndex 发送失败")
            return false
        }
        offset = end
        chunkIndex++
    }
    return true
}

/**
 * Collects incoming [BLEPacket]s from [BLEConnection.packets], accumulating JSON and
 * binary body bytes until an [BLEPacketType.END] packet arrives, then decodes and returns
 * the complete [ProfileBeacon].
 *
 * Must be called after [BLEConnection.subscribeToNotifications] is enabled.
 */
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun BLEConnection.receiveProfileBeacon(json: Json): Pair<ProfileBeacon, ByteArray?>? {
    val jsonBuf = ByteArrayOutputStream()
    val binaryBuf = ByteArrayOutputStream()

    // onEach runs before first()'s predicate; END packet is accumulated first, then predicate fires
    packets
        .onEach { packet ->
            when (packet.type) {
                BLEPacketType.JSON -> jsonBuf.write(packet.body)
                BLEPacketType.BINARY -> binaryBuf.write(packet.body)
            }
        }
        .first { it.type == BLEPacketType.END }

    val profile = runCatching {
        json.decodeFromString<ProfileBeacon>(
            String(jsonBuf.toByteArray(), Charsets.UTF_8)
        )
    }.getOrNull() ?: run {
        Log.e(BLEConnectionManager.TAG, "ProfileBeacon JSON 解析失败")
        return null
    }

    val avatar = binaryBuf.toByteArray().takeIf { it.isNotEmpty() }
    return Pair(profile, avatar)
}