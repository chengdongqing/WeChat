package top.chengdongqing.wechat.features.contacts.detail.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

/**
 * 朋友圈照片预览项
 */
@Composable
fun MomentPhotosSection(
    photoResIds: List<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Text(
                text = "朋友圈",
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(80.dp)
            )

            // 照片预览网格
            MomentPhotoGrid(photoResIds = photoResIds)
        }

        Icon(
            painter = painterResource(R.drawable.ic_right_outlined),
            contentDescription = "查看朋友圈",
            tint = WeTheme.colorScheme.textSecondary
        )
    }
}

/**
 * 朋友圈照片网格
 */
@Composable
private fun MomentPhotoGrid(
    photoResIds: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photoResIds.take(3).forEach { photoResId ->
            MomentPhotoThumbnail(photoResId = photoResId)
        }
    }
}

/**
 * 朋友圈照片缩略图
 */
@Composable
private fun MomentPhotoThumbnail(
    @DrawableRes photoResId: Int,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(photoResId),
        contentDescription = "朋友圈照片",
        contentScale = ContentScale.Crop,
        modifier = modifier.size(48.dp)
    )
}