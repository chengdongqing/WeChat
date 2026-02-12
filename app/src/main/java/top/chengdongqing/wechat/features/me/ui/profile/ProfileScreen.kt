package top.chengdongqing.wechat.features.me.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.navigation.MeRoute

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

    Scaffold(
        topBar = {
            WeTopBar(title = "个人资料", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 资料内容
            ProfileContent(
                modifier = Modifier.padding(innerPadding),
                profile = uiState.profile,
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
                }
            )

            // 错误提示
            WeInformationBar(
                visible = uiState.error != null,
                message = uiState.error ?: "",
                type = InformationBarType.WarnStrong,
                autoClose = true,
                onClose = { viewModel.clearError() }
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
    onNavigateToAvatarEdit: () -> Unit,
    onNavigateToNameEdit: () -> Unit,
    onNavigateToGenderEdit: () -> Unit,
    onNavigateToIdView: () -> Unit,
    onNavigateToQRCode: () -> Unit,
    onNavigateToSignatureEdit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 基本信息组
        Column {
            // 头像
            ProfileItem(
                label = "头像",
                onClick = onNavigateToAvatarEdit
            ) {
                AvatarContent(profile?.avatarPath)
            }

            // 名字
            ProfileItem(
                label = "名字",
                onClick = onNavigateToNameEdit
            ) {
                TextContent(profile?.nickname)
            }

            // 性别
            ProfileItem(
                label = "性别",
                onClick = onNavigateToGenderEdit
            ) {
                TextContent(profile?.gender?.label)
            }

            // 微信号
            ProfileItem(
                label = "微信号",
                onClick = onNavigateToIdView
            ) {
                TextContent(profile?.id)
            }

            // 二维码
            ProfileItem(
                label = "我的二维码",
                onClick = onNavigateToQRCode
            ) {
                QRCodeContent()
            }

            // 签名
            ProfileItem(
                label = "签名",
                showDivider = false,
                onClick = onNavigateToSignatureEdit
            ) {
                TextContent(profile?.signature)
            }
        }

        // 其他设置
        ProfileItem(
            label = "来电铃声",
            showDivider = false,
            onClick = null
        ) {}
    }
}

/**
 * 个人资料项组件
 */
@Composable
private fun ProfileItem(
    label: String,
    showDivider: Boolean = true,
    onClick: (() -> Unit)?,
    trailing: @Composable RowScope.() -> Unit
) {
    Column(modifier = Modifier.background(White)) {
        MenuListItem(
            label = label,
            trailing = trailing,
            onClick = onClick
        )

        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 16.dp))
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

/**
 * 文本内容
 */
@Composable
private fun RowScope.TextContent(text: String?) {
    text?.let {
        Text(
            text = text,
            fontSize = 16.sp,
            color = WeTheme.colorScheme.textSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}