package top.chengdongqing.wechat.feature.contacts.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.contacts.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.LocalAiAssistant

@Composable
fun ContactDetailContent(
    contact: Contact,
    onAction: (ContactAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 基本信息
        BasicInfoSection(contact) {
            onAction(ContactAction.ViewProfile)
        }

        // 朋友圈
        if (contact.isFriend || contact.isSelf) {
            MomentPhotoSection {
                onAction(ContactAction.ViewMoments)
            }
        }

        // 操作按钮
        ActionButtonSection(
            contact,
            onAction = onAction
        )

        // 加入黑名单后的提示文字
        if (contact.isBlocked) {
            BlockedHint()
        }
    }
}

@Composable
fun LocalAiContactDetailContent(onSendMessage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = DesignR.drawable.img_logo,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = LocalAiAssistant.NAME,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = LocalAiAssistant.SIGNATURE,
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "小微同学是运行在本机的 AI 助手，模型和对话推理均保留在你的设备上。",
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(WeTheme.colorScheme.surface)
                .clickable(onClick = onSendMessage),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_message_outlined),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF576B95),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.contact_action_send_message),
                color = androidx.compose.ui.graphics.Color(0xFF576B95),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BlockedHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.contact_blocked_hint),
            color = WeTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

/**
 * 基本信息板块
 */
@Composable
private fun BasicInfoSection(
    contact: Contact,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier.background(WeTheme.colorScheme.surface)
    ) {
        // 头像和基本信息
        ContactBasicInfoCard(contact = contact)

        Spacer(modifier = Modifier.height(12.dp))

        if (!contact.isSelf) {
            WeDivider(modifier = Modifier.padding(start = 16.dp))

            // 朋友资料信息
            ContactProfileItem(
                contact = contact,
                onClick = onProfileClick
            )
        }
    }
}
