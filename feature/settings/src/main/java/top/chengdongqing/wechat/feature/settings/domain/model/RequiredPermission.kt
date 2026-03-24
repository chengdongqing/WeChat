package top.chengdongqing.wechat.feature.settings.domain.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R

/**
 * 系统权限
 */
enum class RequiredPermission(
    @get:StringRes val labelRes: Int,
    @get:StringRes val descriptionRes: Int
) {
    Location(R.string.permission_location, R.string.permission_location_desc),
    Microphone(R.string.permission_microphone, R.string.permission_microphone_desc),
    Camera(R.string.permission_camera, R.string.permission_camera_desc),
    NFC(R.string.permission_nfc, R.string.permission_nfc_desc),
    Bluetooth(R.string.permission_bluetooth, R.string.permission_bluetooth_desc),
    WiFi(R.string.permission_wifi, R.string.permission_wifi_desc);
}