package top.chengdongqing.wechat.ui.contacts.detail.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.Contact
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.ui.theme.WeChatTheme

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
            .padding(16.dp)
    ) {
        // 头像
        ContactAvatar(
            avatarResId = R.drawable.img_avatar,
            contentDescription = "头像"
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 联系人信息
        ContactBasicInfo(contact = contact)
    }
}

/**
 * 联系人头像组件
 */
@Composable
private fun ContactAvatar(
    @DrawableRes avatarResId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(avatarResId),
        contentDescription = contentDescription,
        modifier = modifier
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
            name = contact.remarkName,
            gender = contact.gender
        )

        // 昵称
        if (contact.name.isNotEmpty()) {
            InfoText(
                label = "昵称：",
                value = contact.name,
                modifier = Modifier.padding(top = 2.dp)
            )
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
    gender: Gender,
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

        if (gender != Gender.Unknown) {
            Spacer(modifier = Modifier.width(4.dp))
            GenderIcon(gender = gender)
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
        Gender.Unknown -> return
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
        color = WeChatTheme.colorScheme.textSecondary,
        fontSize = 14.sp,
        modifier = modifier
    )
}