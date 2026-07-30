package top.chengdongqing.wechat.core.designsystem.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.ContactAddSource

@get:StringRes
val ContactAddSource.labelRes: Int
    get() = when (this) {
        ContactAddSource.Search -> R.string.add_source_search
        ContactAddSource.QRCode -> R.string.add_source_qrcode
        ContactAddSource.Bump -> R.string.add_source_bump
        ContactAddSource.Radar -> R.string.add_source_radar
        ContactAddSource.Group -> R.string.add_source_group
        ContactAddSource.Card -> R.string.add_source_card
    }

fun ContactAddSource.getDescription(resources: Resources, isFromMe: Boolean): String {
    val label = resources.getString(this.labelRes)
    return if (isFromMe) {
        resources.getString(R.string.add_source_desc_from_me, label)
    } else {
        resources.getString(R.string.add_source_desc_from_other, label)
    }
}
