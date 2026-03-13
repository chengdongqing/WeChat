package top.chengdongqing.wechat.features.chat.ui.preview.file

import android.text.format.Formatter.formatFileSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun FilePreviewScreen(
    messageId: String, onBack: () -> Unit,
    viewModel: FilePreviewViewModel = hiltViewModel { factory: FilePreviewViewModel.Factory ->
        factory.create(messageId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            WeTopBar(onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 80.dp, horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_file_filled),
                    contentDescription = null,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = uiState.filename,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeTheme.colorScheme.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(
                        R.string.message_file_size,
                        formatFileSize(context, uiState.fileSize)
                    ),
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 17.sp
                )

                if (!uiState.fileExists) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.message_file_not_found),
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WeButton(
                    text = stringResource(R.string.action_open),
                    type = ButtonType.Plain,
                    enabled = uiState.fileExists
                ) {
                    viewModel.openFile()
                }
                WeButton(
                    text = stringResource(R.string.action_save),
                    loading = uiState.isSaving,
                    enabled = uiState.fileExists
                ) {
                    viewModel.saveFile()
                }
            }
        }
    }
}