package top.chengdongqing.wechat.core.common.nfc

object NfcConstants {
    /** 应用标识符（AID），读卡方凭此定位到本应用 */
    val HCE_AID = byteArrayOf(
        0xF0.toByte(), 0x57, 0x65, 0x43,
        0x68, 0x61, 0x74, 0x4E, 0x46, 0x43
    )

    /** HCE 服务完整类名 */
    const val HCE_SERVICE_CLASS = "top.chengdongqing.wechat.core.nfc.NfcHceService"
}
