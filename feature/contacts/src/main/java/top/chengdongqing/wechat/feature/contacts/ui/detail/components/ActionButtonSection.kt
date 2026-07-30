package top.chengdongqing.wechat.feature.contacts.ui.detail.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.call.CallOptions
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.feature.contacts.ui.detail.ContactAction

/**
 * 操作按钮板块
 * 包含发消息和音视频通话按钮
 */
@Composable
fun ActionButtonSection(
    contact: Contact,
    onAction: (ContactAction) -> Unit
) {
    val actionSheet = rememberActionSheetState()

    Column(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
        when {
            contact.isSelf || contact.isFriend -> {
                ActionButton(
                    icon = R.drawable.ic_message_outlined,
                    text = stringResource(R.string.contact_action_send_message),
                    onClick = { onAction(ContactAction.SendMessage) }
                )
                if (contact.isFriend && !contact.isBlocked) {
                    WeDivider()
                    ActionButton(
                        icon = R.drawable.ic_voice_video_outlined,
                        text = stringResource(R.string.contact_action_voice_video_call),
                        onClick = {
                            actionSheet.show(CallOptions) { index ->
                                val callType = when (index) {
                                    0 -> CallType.Video
                                    else -> CallType.Voice
                                }
                                onAction(ContactAction.VoiceVideoCall(callType))
                            }
                        }
                    )
                }
            }

            else -> {
                ActionButton(
                    text = stringResource(R.string.contact_action_add_to_contacts),
                    onClick = { onAction(ContactAction.AddToContacts) }
                )
            }
        }
    }
}

/**
 * 联系人操作按钮
 */
@Composable
private fun ActionButton(
    @DrawableRes icon: Int? = null,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = Color(0xFF576B95),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Color(0xFF576B95),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}