package top.chengdongqing.wechat.data.network.ble

import java.util.UUID

/**
 * Shared BLE configuration: UUIDs, size limits, and timeouts.
 * Single source of truth for all BLE-related constants.
 */
object BLEConfig {

    // ── BLE identifiers ──────────────────────────────────────────────────────
    val SERVICE_UUID: UUID = UUID.fromString("0000FE9F-0000-1000-8000-00805F9B34FB")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FEA0-0000-1000-8000-00805F9B34FB")
    val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ── Packet / data limits ─────────────────────────────────────────────────
    const val MAX_PACKET_BODY = 500       // max body bytes per BlePacket chunk
    const val USER_ID_HASH_LENGTH = 4     // bytes from MD5 used in advertising payload
    const val AVATAR_THUMBNAIL_SIZE = 100 // target thumbnail width/height in px
    const val AVATAR_MAX_SIZE_KB = 5      // max compressed avatar size
    const val MTU_SIZE = 512

    // ── Timeouts (ms) ────────────────────────────────────────────────────────
    const val SCAN_TIMEOUT_MS = 10_000L
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val WRITE_TIMEOUT_MS = 10_000L
    const val READ_TIMEOUT_MS = 30_000L
    const val CLOSE_DELAY_MS = 500L
}