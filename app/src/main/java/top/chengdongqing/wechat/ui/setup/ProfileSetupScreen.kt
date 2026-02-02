package top.chengdongqing.wechat.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.theme.White

/**
 * 个人资料首次设置页面
 * 用于用户首次使用时配置昵称和头像
 */
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = viewModel(),
    onBack: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            WeTopBar("", containerColor = White, onBack = onBack)
        },
        containerColor = White
    ) { paddingValues ->
        ProfileSetupContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onUserNameChange = { viewModel.onUserNameChange(it) },
            onCompleteClick = {
                viewModel.saveProfile()
                onSetupComplete()
            }
        )
    }
}

@Composable
private fun ProfileSetupContent(
    modifier: Modifier = Modifier,
    uiState: ProfileSetupUiState,
    onUserNameChange: (String) -> Unit,
    onCompleteClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 标题
        Text(
            text = "个人资料设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "设置昵称和头像，让朋友认识你",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 头像
        AvatarSection(
            avatarUri = uiState.avatarUri,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 昵称输入
        OutlinedTextField(
            value = uiState.userName,
            onValueChange = onUserNameChange,
            label = { Text("昵称") },
            placeholder = { Text("请输入昵称") },
            singleLine = true,
            isError = uiState.userNameError != null,
            supportingText = {
                uiState.userNameError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 设备ID（只读显示）
        DeviceIdDisplay(deviceId = uiState.deviceId)

        Spacer(modifier = Modifier.height(32.dp))

        WeButton(
            text = "确定",
            disabled = !uiState.isValid,
            loading = uiState.isSaving
        ) {
            onCompleteClick()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 提示信息
        PrivacyHint()
    }
}

@Composable
private fun AvatarSection(
    avatarUri: android.net.Uri?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.img_avatar_placeholder),
                    contentDescription = "选择头像",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = if (avatarUri == null) "点击设置头像" else "点击更换头像",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun DeviceIdDisplay(deviceId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "设备标识",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = deviceId.take(16) + "...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PrivacyHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "局域网模式说明",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "你的资料仅在局域网内可见，不会上传到互联网",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}