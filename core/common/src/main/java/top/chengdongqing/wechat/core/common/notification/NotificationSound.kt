package top.chengdongqing.wechat.core.common.notification

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import top.chengdongqing.wechat.core.common.R

enum class NotificationSound(
    @get:StringRes val labelRes: Int,
    val soundRes: Int?
) {
    FollowSystem(R.string.settings_follow_system, null),
    Blocks(R.string.notification_sound_blocks, R.raw.notification_crystal),
    Cute(R.string.notification_sound_cute, R.raw.notification_moment),
    Ethereal(R.string.notification_sound_ethereal, R.raw.notification_fresh),
    Playful(R.string.notification_sound_playful, R.raw.notification_xylophone),
    Crisp(R.string.notification_sound_crisp, R.raw.notification_fade_in),
    Nimble(R.string.notification_sound_nimble, R.raw.notification_fade_out);
}

fun NotificationSound.toUri(context: Context): Uri =
    if (soundRes != null) {
        "android.resource://${context.packageName}/$soundRes".toUri()
    } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
