package top.chengdongqing.wechat.feature.contacts.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun MomentPhotoSection(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WeTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.contact_moments_title),
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(80.dp)
            )

            // 图片列表
            MomentPhotoRow()
        }

        Icon(
            painter = painterResource(R.drawable.ic_right_outlined),
            contentDescription = null,
            tint = WeTheme.colorScheme.textSecondary,
            modifier = Modifier
                .size(24.dp)
                .offset(x = 8.dp)
        )
    }
}

@Composable
private fun MomentPhotoRow() {
    val photoResIds = remember {
        listOf(R.drawable.img_avatar, R.drawable.img_splash, R.drawable.img_radar_bg)
    }

    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photoResIds.take(3).forEach { photoResId ->
            Image(
                painter = painterResource(photoResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}