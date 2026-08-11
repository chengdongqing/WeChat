package top.chengdongqing.wechat.feature.contacts.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.contacts.R

/**
 * 添加朋友的方式
 */
enum class AddContactOption(
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int,
    @get:DrawableRes val icon: Int,
    val iconColor: Color
) {
    Scan(
        titleRes = R.string.add_contact_option_scan_title,
        descriptionRes = R.string.add_contact_option_scan_desc,
        icon = DesignR.drawable.ic_scan_outlined,
        iconColor = Color(0xFF2B7CF1)
    ),
    Nfc(
        titleRes = R.string.add_contact_option_nfc_title,
        descriptionRes = R.string.add_contact_option_nfc_desc,
        icon = DesignR.drawable.ic_nfc_outlined,
        iconColor = Color(0xFF10AEFF)
    ),
    Radar(
        titleRes = R.string.add_contact_option_radar_title,
        descriptionRes = R.string.add_contact_option_radar_desc,
        icon = DesignR.drawable.ic_radar_outlined,
        iconColor = Color(0xFF7468BE)
    ),
    FaceToFaceGroup(
        titleRes = R.string.add_contact_option_face_to_face_title,
        descriptionRes = R.string.add_contact_option_face_to_face_desc,
        icon = DesignR.drawable.ic_group_chat_outlined,
        iconColor = Color(0xFF07C160)
    )
}
