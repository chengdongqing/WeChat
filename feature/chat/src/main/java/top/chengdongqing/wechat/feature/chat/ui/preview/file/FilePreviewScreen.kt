package top.chengdongqing.wechat.feature.chat.ui.preview.file

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.file.PublicFileManager
import top.chengdongqing.wechat.core.file.openFile
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.feature.chat.R
import java.io.File
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun FilePreviewScreen(
    messageId: String, onBack: () -> Unit,
    viewModel: FilePreviewViewModel = hiltViewModel { factory: FilePreviewViewModel.Factory ->
        factory.create(messageId)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FilePreviewPage(state, onBack, viewModel::openFile, viewModel::saveFile)
}

@Composable
fun FilePreviewScreen(file: MessageContent.File, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val source = remember(file.localPath) { File(file.localPath) }
    var saving by remember { mutableStateOf(false) }
    val state = FilePreviewUiState(
        filename = file.filename,
        fileSize = file.size,
        mimeType = file.mimeType,
        localPath = file.localPath,
        isLoading = false,
        isSaving = saving
    )

    FilePreviewPage(
        uiState = state,
        onBack = onBack,
        onOpen = {
            runCatching {
                context.openFile(source, file.mimeType)
            }.onFailure {
                context.showToast("没有找到可以打开此文件的应用")
            }
        },
        onSave = {
            scope.launch {
                saving = true
                val saved = PublicFileManager(context)
                    .saveMedia(MessageType.File, source, file.filename)
                context.showToast(if (saved != null) "已保存" else "保存失败")
                saving = false
            }
        }
    )
}

@Composable
private fun FilePreviewPage(
    uiState: FilePreviewUiState,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            WeTopAppBar(onBack = onBack)
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
                    painter = painterResource(DesignR.drawable.ic_file_filled),
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
                    text = stringResource(DesignR.string.action_open),
                    type = ButtonType.Plain,
                    enabled = uiState.fileExists
                ) {
                    onOpen()
                }
                WeButton(
                    text = stringResource(DesignR.string.action_save),
                    loading = uiState.isSaving,
                    enabled = uiState.fileExists
                ) {
                    onSave()
                }
            }
        }
    }
}
