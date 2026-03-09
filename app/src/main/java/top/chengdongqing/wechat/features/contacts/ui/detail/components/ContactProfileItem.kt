package top.chengdongqing.wechat.features.contacts.ui.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.features.contacts.domain.model.Contact

/**
 * 朋友资料信息项
 * 包含标签和备注信息
 */
@Composable
fun ContactProfileItem(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 朋友资料标题行
        ProfileTitleRow()

        if (contact.signature?.isNotBlank().isTrue()) {
            ProfileInfoRow(
                label = "签名",
                value = contact.signature ?: ""
            )
        }
        if (contact.note?.isNotEmpty().isTrue()) {
            ProfileInfoRow(
                label = "备忘",
                value = contact.note!!
            )
        }
    }
}

/**
 * 朋友资料标题行
 */
@Composable
private fun ProfileTitleRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "朋友资料",
            modifier = Modifier.width(80.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Icon(
            painter = painterResource(R.drawable.ic_right_outlined),
            contentDescription = "查看详情",
            tint = Color.DarkGray,
            modifier = Modifier
                .size(24.dp)
                .offset(x = 8.dp)
        )
    }
}

/**
 * 资料信息行（标签、备注等）
 */
@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 14.sp,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 14.sp
        )
    }
}