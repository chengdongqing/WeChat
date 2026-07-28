package top.chengdongqing.wechat.feature.settings.ui.storage

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

private val WeGreen = Color(0xFF07C160)
private val OtherAppsYellow = Color(0xFFFFB900)
private val RemainingGray = Color(0xFFD5D5D5)

@Composable
fun StorageSettingsScreen(
    onBack: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingClean by remember { mutableStateOf<StorageCategory?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = { WeTopAppBar(title = "存储空间", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WeGreen)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                StorageBar(state)
                StorageLegend()
                Spacer(Modifier.height(12.dp))
                Text("微信已用空间", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(
                    Formatter.formatFileSize(context, state.appBytes),
                    fontSize = 39.sp,
                    fontWeight = FontWeight.Medium
                )
                val percent = if (state.totalBytes == 0L) 0
                else (state.appBytes * 100 / state.totalBytes).coerceIn(0, 100)
                Text("占手机 $percent% 存储空间", color = Color(0xFF777777), fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))

                StorageCard(
                    title = "缓存",
                    bytes = state.cacheBytes,
                    description = "缓存是使用微信过程中产生的临时数据，清理缓存不会影响微信的正常使用。",
                    category = StorageCategory.Cache,
                    cleaning = state.cleaning,
                    emphasized = true,
                    onClean = { pendingClean = it }
                )
                StorageCard(
                    title = "聊天记录",
                    bytes = state.chatBytes,
                    description = "可清理聊天记录里的图片、视频和文件，文字消息会继续保留。",
                    category = StorageCategory.Chats,
                    cleaning = state.cleaning,
                    onClean = { pendingClean = it }
                )
                StorageCard(
                    title = "资源文件",
                    bytes = state.resourceBytes,
                    description = "包含部分功能运行时所需、可重新生成或下载的资源文件。",
                    category = StorageCategory.Resources,
                    cleaning = state.cleaning,
                    onClean = { pendingClean = it }
                )
                StorageCard(
                    title = "必要文件",
                    bytes = state.necessaryBytes,
                    description = "包含微信运行所需的必要文件，该类别的大小因当前使用状态而异。",
                    category = null,
                    cleaning = state.cleaning,
                    onClean = {}
                )
                Spacer(Modifier.height(8.dp))
                Text("其他账号占用了 0 B", fontSize = 16.sp)
                Text(
                    "包含在当前设备登录过的其他账号的聊天记录和登录记录。",
                    color = Color(0xFF777777),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    pendingClean?.let { category ->
        val label = when (category) {
            StorageCategory.Cache -> "缓存"
            StorageCategory.Chats -> "聊天记录中的媒体文件"
            StorageCategory.Resources -> "资源文件"
        }
        AlertDialog(
            onDismissRequest = { pendingClean = null },
            title = { Text("清理$label？") },
            text = {
                Text(
                    if (category == StorageCategory.Chats)
                        "图片、视频、语音和文件将从本机删除，文字消息不会被删除。"
                    else "清理后，使用相关功能时可能会重新生成或下载这些文件。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingClean = null
                    viewModel.clean(category)
                }) { Text("清理", color = WeGreen) }
            },
            dismissButton = { TextButton(onClick = { pendingClean = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun StorageBar(state: StorageUiState) {
    val total = state.totalBytes.coerceAtLeast(1)
    val appFraction = (state.appBytes.toFloat() / total).coerceIn(0f, 1f)
    val deviceUsed = (total - state.freeBytes).coerceAtLeast(state.appBytes)
    val otherFraction = ((deviceUsed - state.appBytes).toFloat() / total)
        .coerceIn(0f, 1f - appFraction)
    Row(
        modifier = Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(1.dp))
            .background(RemainingGray)
    ) {
        if (appFraction > 0) {
            Box(Modifier.weight(appFraction.coerceAtLeast(.008f)).fillMaxSize().background(WeGreen))
        }
        if (otherFraction > 0) {
            Box(Modifier.weight(otherFraction).fillMaxSize().background(OtherAppsYellow))
        }
        val remaining = (1f - appFraction - otherFraction).coerceAtLeast(.001f)
        Box(Modifier.weight(remaining).fillMaxSize().background(RemainingGray))
    }
}

@Composable
private fun StorageLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(WeGreen, "微信已用")
        LegendItem(OtherAppsYellow, "其他App已用")
        LegendItem(RemainingGray, "手机剩余可用")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(9.dp).height(9.dp).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color(0xFF666666), fontSize = 13.sp)
    }
}

@Composable
private fun StorageCard(
    title: String,
    bytes: Long,
    description: String,
    category: StorageCategory?,
    cleaning: StorageCategory?,
    emphasized: Boolean = false,
    onClean: (StorageCategory) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(WeTheme.colorScheme.surface).padding(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(
                Formatter.formatFileSize(context, bytes),
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                description,
                color = Color(0xFF777777),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
        if (category != null) {
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onClean(category) },
                enabled = cleaning == null && bytes > 0,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (emphasized) WeGreen else Color(0xFFF1F1F1),
                    contentColor = if (emphasized) Color.White else Color.Black,
                    disabledContainerColor = Color(0xFFF1F1F1),
                    disabledContentColor = Color(0xFFAAAAAA)
                )
            ) {
                if (cleaning == category) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp
                    )
                } else Text("清理", fontWeight = FontWeight.Bold)
            }
        }
    }
}
