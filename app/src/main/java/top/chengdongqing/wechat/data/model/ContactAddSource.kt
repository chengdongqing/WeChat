package top.chengdongqing.wechat.data.model

import android.content.res.Resources
import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

enum class ContactAddSource(
    @get:StringRes val labelRes: Int
) {
    Search(R.string.add_source_search),
    QRCode(R.string.add_source_qrcode),
    Bump(R.string.add_source_bump),
    Radar(R.string.add_source_radar),
    Group(R.string.add_source_group),
    Card(R.string.add_source_card);

    fun getDescription(resources: Resources, isFromMe: Boolean): String {
        val label = resources.getString(labelRes)

        return when (isFromMe) {
            true -> resources.getString(R.string.add_source_desc_from_me, label)
            false -> resources.getString(R.string.add_source_desc_from_other, label)
        }
    }
}