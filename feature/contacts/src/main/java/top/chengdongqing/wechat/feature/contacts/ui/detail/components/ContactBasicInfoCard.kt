package top.chengdongqing.wechat.feature.contacts.ui.detail.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.Black
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.ui.labelRes
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.Gender

@Composable
fun ContactBasicInfoCard(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = if (contact.isSelf) Alignment.CenterVertically else Alignment.Top
    ) {
        ContactAvatar(contact)
        Spacer(modifier = Modifier.width(16.dp))
        ContactBasicInfo(contact)
    }
}

@Composable
private fun ContactAvatar(contact: Contact) {
    var dialogVisible by remember { mutableStateOf(false) }
    val close = { dialogVisible = false }

    AsyncImage(
        model = contact.avatarPath,
        contentDescription = stringResource(R.string.contact_avatar_description),
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .weClickable(enabled = contact.isFriend || contact.isSelf) {
                dialogVisible = true
            }
    )

    /**
     * 头像预览
     */
    if (dialogVisible) {
        Dialog(
            onDismissRequest = close,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ZoomableAsyncImage(
                model = contact.avatarPath,
                contentDescription = stringResource(R.string.me_profile_avatar),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black),
                onClick = { close() }
            )
        }
    }
}

@Composable
private fun ContactBasicInfo(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = contact.displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = WeTheme.colorScheme.textPrimary
            )

            contact.gender?.let {
                Spacer(modifier = Modifier.width(4.dp))
                GenderIcon(it)
            }
        }

        if (!contact.isSelf) {
            InfoText(
                label = stringResource(R.string.contact_label_nickname),
                value = contact.nickname,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        InfoText(
            label = stringResource(R.string.contact_label_wechat_id),
            value = contact.id
        )
    }
}

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
        contentDescription = stringResource(gender.labelRes),
        tint = tint,
        modifier = modifier.size(16.dp)
    )
}

@Composable
private fun InfoText(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$label $value",
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 14.sp,
        modifier = modifier
    )
}