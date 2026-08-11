package top.chengdongqing.wechat.core.playback

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R as DesignR

enum class RingtoneSound(
    @get:StringRes val labelRes: Int,
    val ringtoneRes: Int?
) {
    FollowSystem(DesignR.string.settings_follow_system, null),
    Default(DesignR.string.ringtone_default, R.raw.ringtone_default),
    Mi(DesignR.string.ringtone_mi, R.raw.ringtone_mi),
    MiJazz(DesignR.string.ringtone_mi_jazz, R.raw.ringtone_mi_jazz),
    MiHouse(DesignR.string.ringtone_mi_house, R.raw.ringtone_mi_house),
    MiRemix(DesignR.string.ringtone_mi_remix, R.raw.ringtone_mi_remix);
}

fun RingtoneSound.toUri(context: Context): Uri =
    if (ringtoneRes != null) {
        Uri.parse("android.resource://${context.packageName}/$ringtoneRes")
    } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
