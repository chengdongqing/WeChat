package top.chengdongqing.wechat.features.me.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menu.WeMenuListItem
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.navigation.MeRoute
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel
import top.chengdongqing.wechat.features.settings.navigation.SettingsRoute

@Composable
fun MeScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(WeTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            UserInfoSection(
                profile = uiState.profile,
                onNavigateToProfile = {
                    navController.navigate(MeRoute.PROFILE)
                },
                onNavigateToQRCode = {
                    navController.navigate(MeRoute.QR_CODE)
                }
            )
            StatusSection()
        }
        WeMenuListItem(
            label = stringResource(R.string.me_menu_service),
            icon = R.drawable.ic_pay_logo_outlined,
            iconColor = Color(0xFF07C160),
            onClick = {}
        )
        Column(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
            WeMenuListItem(
                label = stringResource(R.string.me_menu_favorites),
                icon = R.drawable.ic_favorites_outlined_colorful,
                iconColor = Color.Unspecified,
                onClick = {}
            )
            WeDivider(modifier = Modifier.padding(start = 56.dp))
            WeMenuListItem(
                label = stringResource(R.string.me_menu_moments),
                icon = R.drawable.ic_album_outlined,
                iconColor = Color(0xFF2782D7),
                onClick = {}
            )
            WeDivider(modifier = Modifier.padding(start = 56.dp))
            WeMenuListItem(
                label = stringResource(R.string.me_menu_stickers),
                icon = R.drawable.ic_emoji_outlined,
                iconColor = Color(0xFFF9C018),
                onClick = {}
            )
        }
        WeMenuListItem(
            label = stringResource(R.string.me_menu_settings),
            icon = R.drawable.ic_settings_outlined,
            iconColor = Color(0xFF2782D7)
        ) {
            navController.navigate(SettingsRoute.Settings.route)
        }
    }
}

@Composable
fun UserInfoSection(
    profile: UserProfile?,
    onNavigateToProfile: () -> Unit,
    onNavigateToQRCode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .weClickable { onNavigateToProfile() }
            .padding(start = 24.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = profile?.avatarPath,
            contentDescription = "头像",
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile?.nickname ?: "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeTheme.colorScheme.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    painter = painterResource(R.drawable.ic_qrcode_outlined),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .weClickable { onNavigateToQRCode() },
                    tint = Color(0xFF456F6F)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${stringResource(R.string.me_id)}：${profile?.id}",
                    fontSize = 14.sp,
                    color = WeTheme.colorScheme.textSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    painter = painterResource(R.drawable.ic_right_outlined),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 4.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatusSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.width(84.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .border(0.5.dp, WeTheme.colorScheme.divider, CircleShape)
                .clickable { }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus_outlined),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = WeTheme.colorScheme.textSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.me_status),
                fontSize = 12.sp,
                color = WeTheme.colorScheme.textSecondary
            )
        }
    }
}