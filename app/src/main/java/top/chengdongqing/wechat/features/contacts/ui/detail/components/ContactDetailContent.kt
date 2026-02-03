package top.chengdongqing.wechat.features.contacts.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.features.contacts.model.Contact
import top.chengdongqing.wechat.features.contacts.ui.detail.ContactAction

/**
 * 联系人详情内容区域
 */
@Composable
fun ContactDetailContent(
    contact: Contact,
    onAction: (ContactAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 基本信息板块
        BasicInfoSection(
            contact = contact,
            onProfileClick = { onAction(ContactAction.ViewProfile) }
        )

        // 朋友圈板块
        if (contact.momentPhotos.isNotEmpty()) {
            MomentPhotosSection(
                photoResIds = contact.momentPhotos,
                onClick = { onAction(ContactAction.ViewMoments) }
            )
        }

        // 操作按钮板块
        ActionButtonsSection(onAction = onAction)
    }
}

/**
 * 基本信息板块
 */
@Composable
private fun BasicInfoSection(
    contact: Contact,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(Color.White)) {
        // 头像和基本信息
        ContactBasicInfoCard(contact = contact)

        Spacer(modifier = Modifier.height(12.dp))
        WeDivider(modifier = Modifier.padding(start = 16.dp))

        // 朋友资料信息
        ContactProfileItem(
            contact = contact,
            onClick = onProfileClick
        )
    }
}