package top.chengdongqing.wechat.core.notification

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import top.chengdongqing.wechat.core.designsystem.R as DesignR

enum class NotificationSound(
    @get:StringRes val labelRes: Int,
    val soundRes: Int?
) {
    FollowSystem(DesignR.string.settings_follow_system, null),
    Blocks(DesignR.string.notification_sound_blocks, DesignR.raw.notification_crystal),
    Cute(DesignR.string.notification_sound_cute, DesignR.raw.notification_moment),
    Ethereal(DesignR.string.notification_sound_ethereal, DesignR.raw.notification_fresh),
    Playful(DesignR.string.notification_sound_playful, DesignR.raw.notification_xylophone),
    Crisp(DesignR.string.notification_sound_crisp, DesignR.raw.notification_fade_in),
    Nimble(DesignR.string.notification_sound_nimble, DesignR.raw.notification_fade_out);
}

fun NotificationSound.toUri(context: Context): Uri =
    if (soundRes != null) {
        "android.resource://${context.packageName}/$soundRes".toUri()
    } else {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
