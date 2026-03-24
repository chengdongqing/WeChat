package top.chengdongqing.wechat.core.common.media

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.common.R

enum class RingtoneSound(
    @get:StringRes val labelRes: Int,
    val ringtoneRes: Int?
) {
    FollowSystem(R.string.settings_follow_system, null),
    Default(R.string.ringtone_default, R.raw.ringtone_default),
    Mi(R.string.ringtone_mi, R.raw.ringtone_mi),
    MiJazz(R.string.ringtone_mi_jazz, R.raw.ringtone_mi_jazz),
    MiHouse(R.string.ringtone_mi_house, R.raw.ringtone_mi_house),
    MiRemix(R.string.ringtone_mi_remix, R.raw.ringtone_mi_remix);
}

fun RingtoneSound.toUri(context: Context): Uri =
    if (ringtoneRes != null) {
        Uri.parse("android.resource://${context.packageName}/$ringtoneRes")
    } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
