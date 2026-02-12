package top.chengdongqing.wechat.features.contacts.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.me.domain.model.Gender

/**
 * 联系人头像和基本信息卡片
 */
@Composable
fun ContactBasicInfoCard(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = if (contact.isMyself) Alignment.CenterVertically else Alignment.Top
    ) {
        ContactAvatar(contact.avatarPath ?: R.drawable.img_avatar_placeholder)
        Spacer(modifier = Modifier.width(16.dp))
        ContactBasicInfo(contact)
    }
}

/**
 * 联系人头像组件
 */
@Composable
private fun ContactAvatar(avatarUrl: Any) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = "头像",
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

/**
 * 联系人基本信息（姓名、性别、昵称、微信号）
 */
@Composable
private fun ContactBasicInfo(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 姓名和性别
        NameWithGender(
            name = contact.displayName,
            gender = contact.gender
        )

        // 昵称
        if (!contact.isMyself) {
            InfoText(
                label = "昵称：",
                value = contact.nickname,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 微信号
        InfoText(
            label = "微信号：",
            value = contact.id
        )
    }
}

/**
 * 姓名和性别组合组件
 */
@Composable
private fun NameWithGender(
    name: String,
    gender: Gender?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        if (gender != null) {
            Spacer(modifier = Modifier.width(4.dp))
            GenderIcon(gender)
        }
    }
}

/**
 * 性别图标
 */
@Composable
private fun GenderIcon(
    gender: Gender,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (gender) {
        Gender.Female -> R.drawable.ic_female_filled to Color(0xFFFF5252)
        Gender.Male -> R.drawable.ic_male_filled to Color(0xFF2196F3)
    }

    Icon(
        painter = painterResource(icon),
        contentDescription = gender.name,
        tint = tint,
        modifier = modifier.size(16.dp)
    )
}

/**
 * 信息文本组件（用于显示昵称、微信号等）
 */
@Composable
private fun InfoText(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$label$value",
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 14.sp,
        modifier = modifier
    )
}