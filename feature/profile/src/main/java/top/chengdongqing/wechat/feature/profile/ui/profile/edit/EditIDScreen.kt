package top.chengdongqing.wechat.feature.profile.ui.profile.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.profile.ui.profile.ProfileViewModel

@Composable
fun EditIDScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WeTopBar(
                containerColor = WeTheme.colorScheme.surface,
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Image(
                painter = painterResource(R.drawable.img_logo_gray),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                alpha = if (LocalAppearanceSetting.current.isDarkTheme) 0.4f else 0.15f
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "${stringResource(R.string.me_profile_wechat_id)}：${uiState.profile?.id}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = WeTheme.colorScheme.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.me_edit_id_hint),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = WeTheme.colorScheme.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            WeButton(
                text = stringResource(R.string.action_back),
                type = ButtonType.Plain,
                onClick = onBack
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}