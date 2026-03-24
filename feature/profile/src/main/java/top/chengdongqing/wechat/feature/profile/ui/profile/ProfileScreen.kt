package top.chengdongqing.wechat.feature.profile.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.media.RingtoneSound
import top.chengdongqing.wechat.core.common.navigation.MeRoute
import top.chengdongqing.wechat.core.common.navigation.SettingsRoute
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingValue
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.model.UserProfile

/**
 * 个人资料页面
 *
 * 展示当前用户的完整资料信息
 * 提供各项资料的编辑入口
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ringtone by viewModel.ringtone.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(title = stringResource(R.string.me_profile), onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 资料内容
            ProfileContent(
                modifier = Modifier.padding(innerPadding),
                profile = uiState.profile,
                ringtone = ringtone,
                onNavigateToAvatarEdit = {
                    navController.navigate(MeRoute.Edit.AVATAR)
                },
                onNavigateToNameEdit = {
                    navController.navigate(MeRoute.Edit.NAME)
                },
                onNavigateToGenderEdit = {
                    navController.navigate(MeRoute.Edit.GENDER)
                },
                onNavigateToIdView = {
                    navController.navigate(MeRoute.Edit.ID)
                },
                onNavigateToQRCode = {
                    navController.navigate(MeRoute.QR_CODE)
                },
                onNavigateToSignatureEdit = {
                    navController.navigate(MeRoute.Edit.SIGNATURE)
                },
                onNavigateToRingtoneSetting = {
                    navController.navigate(SettingsRoute.RingtoneSetting.route)
                }
            )
        }
    }
}

/**
 * 资料内容区域
 */
@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    profile: UserProfile?,
    ringtone: RingtoneSound,
    onNavigateToAvatarEdit: () -> Unit,
    onNavigateToNameEdit: () -> Unit,
    onNavigateToGenderEdit: () -> Unit,
    onNavigateToIdView: () -> Unit,
    onNavigateToQRCode: () -> Unit,
    onNavigateToSignatureEdit: () -> Unit,
    onNavigateToRingtoneSetting: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                state = rememberScrollState(),
                overscrollEffect = rememberBounceOverscrollEffect()
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 基本信息组
        WeSettingGroup {
            // 头像
            WeSettingItem(
                label = stringResource(R.string.me_profile_avatar),
                onClick = onNavigateToAvatarEdit
            ) {
                AvatarContent(profile?.avatarPath)
            }

            // 名字
            WeSettingItem(
                label = stringResource(R.string.me_profile_name),
                onClick = onNavigateToNameEdit
            ) {
                WeSettingValue(profile?.nickname)
            }

            // 性别
            WeSettingItem(
                label = stringResource(R.string.me_profile_gender),
                onClick = onNavigateToGenderEdit
            ) {
                WeSettingValue(profile?.gender?.labelRes?.let { stringResource(it) })
            }

            // 微信号
            WeSettingItem(
                label = stringResource(R.string.me_profile_wechat_id),
                onClick = onNavigateToIdView
            ) {
                WeSettingValue(profile?.id)
            }

            // 二维码
            WeSettingItem(
                label = stringResource(R.string.me_profile_qrcode),
                onClick = onNavigateToQRCode
            ) {
                QRCodeContent()
            }

            // 签名
            WeSettingItem(
                label = stringResource(R.string.me_profile_bio),
                showDivider = false,
                onClick = onNavigateToSignatureEdit
            ) {
                WeSettingValue(
                    text = profile?.signature,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
        }

        // 其他设置
        WeSettingItem(
            label = stringResource(R.string.me_profile_ringtone),
            showDivider = false,
            onClick = onNavigateToRingtoneSetting
        ) {
            WeSettingValue(stringResource(ringtone.labelRes))
        }
    }
}

/**
 * 头像内容
 */
@Composable
private fun AvatarContent(avatarPath: String?) {
    AsyncImage(
        model = avatarPath,
        contentDescription = "头像",
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp)),
        contentScale = ContentScale.Crop
    )
}

/**
 * 二维码内容
 */
@Composable
private fun QRCodeContent() {
    Icon(
        painter = painterResource(R.drawable.ic_qrcode_outlined),
        contentDescription = "二维码",
        modifier = Modifier.size(24.dp),
        tint = WeTheme.colorScheme.textSecondary
    )
}