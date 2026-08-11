package top.chengdongqing.wechat.feature.contacts.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.feature.contacts.R

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

        // AI助手说明
        if (contact.isAI) {
            AiAssistantDescriptionSection()
        }

        // 操作按钮
        ActionButtonSection(
            contact,
            onAction = onAction
        )

        // 加入黑名单后的提示文字
        if (contact.isBlocked) {
            BlockedNotice()
        }
    }
}

@Composable
private fun AiAssistantDescriptionSection() {
    Column(
        modifier = Modifier
            .background(WeTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.contact_ai_assistant_description),
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun BlockedNotice() {
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

        if (contact.isFriend || contact.isStranger) {
            WeDivider(modifier = Modifier.padding(start = 16.dp))

            // 朋友资料信息
            ContactProfileItem(
                contact = contact,
                onClick = onProfileClick
            )
        }
    }
}
