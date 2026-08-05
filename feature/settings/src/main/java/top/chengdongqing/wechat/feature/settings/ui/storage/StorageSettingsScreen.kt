package top.chengdongqing.wechat.feature.settings.ui.storage

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun StorageSettingsScreen(
    onBack: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { WeTopAppBar(title = "存储空间", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (state.loading) {
                WeLoading(size = 24.dp)
            } else {
                StorageSettingsContent(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun StorageSettingsContent(
    state: StorageUiState,
    viewModel: StorageSettingsViewModel
) {
    val dialog = rememberDialogState()

    fun handleClean(category: StorageCategory) {
        val label = when (category) {
            StorageCategory.Cache -> "缓存"
            StorageCategory.Chats -> "聊天记录中的媒体文件"
            StorageCategory.Resources -> "资源文件"
        }

        dialog.show(
            title = "确定清理${label}吗？",
            onOk = {
                viewModel.clean(category)
            }
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(
                state = rememberScrollState(),
                overscrollEffect = rememberBouncedOverscrollEffect()
            )
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Spacer(Modifier.height(12.dp))
            StorageBar(state)
            Spacer(Modifier.height(6.dp))
            StorageLegend()
        }
        CurrentUsedStorage(state)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StorageCard(
                title = "缓存",
                bytes = state.cacheBytes,
                description = "缓存是使用微信过程中产生的临时数据，清理缓存不会影响微信的正常使用。",
                category = StorageCategory.Cache,
                cleaning = state.cleaning,
                emphasized = true,
                onClean = ::handleClean
            )
            StorageCard(
                title = "聊天记录",
                bytes = state.chatBytes,
                description = "可清理聊天记录里的图片、视频和文件，文字消息会继续保留。",
                category = StorageCategory.Chats,
                cleaning = state.cleaning,
                onClean = ::handleClean
            )
            StorageCard(
                title = "资源文件",
                bytes = state.resourceBytes,
                description = "包含部分功能运行时所需、可重新生成或下载的资源文件。",
                category = StorageCategory.Resources,
                cleaning = state.cleaning,
                onClean = ::handleClean
            )
            StorageCard(
                title = "必要文件",
                bytes = state.necessaryBytes,
                description = "包含微信运行所需的必要文件，该类别的大小因当前使用状态而异。",
                category = null,
                cleaning = state.cleaning,
                onClean = {}
            )
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun CurrentUsedStorage(state: StorageUiState) {
    val context = LocalContext.current

    val percent = remember(state.totalBytes, state.appBytes) {
        when {
            state.totalBytes == 0L -> 0
            else -> (state.appBytes * 100 / state.totalBytes).coerceIn(0, 100)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "微信已用空间",
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = Formatter.formatFileSize(context, state.appBytes),
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "占手机 $percent% 存储空间",
            color = WeTheme.colorScheme.textTertiary,
            fontSize = 13.sp
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
    val remainingFraction = (1f - appFraction - otherFraction)

    val primaryColor = WeTheme.colorScheme.primary
    val chartItems = remember(appFraction, otherFraction, remainingFraction) {
        listOf(
            Pair(appFraction, primaryColor),
            Pair(otherFraction, OtherAppsYellow),
            Pair(remainingFraction, RemainingGray)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        chartItems.forEach { item ->
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(item.first.coerceAtLeast(.008f))
                    .background(item.second)
            )
        }
    }
}

@Composable
private fun StorageLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(WeTheme.colorScheme.primary, "微信已用")
        LegendItem(OtherAppsYellow, "其他App已用")
        LegendItem(RemainingGray, "手机剩余可用")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(8.dp)
                .height(8.dp)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 12.sp
        )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WeTheme.colorScheme.surface)
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = title,
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 13.sp
            )
            Text(
                text = remember(bytes) { Formatter.formatFileSize(context, bytes) },
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = WeTheme.colorScheme.textTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (category != null) {
            WeButton(
                text = "清理",
                size = ButtonSize.Small,
                type = if (emphasized) ButtonType.Primary else ButtonType.Plain,
                loading = cleaning == category,
                enabled = cleaning == null && bytes > 0,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                onClean(category)
            }
        }
    }
}

private val OtherAppsYellow = Color(0xFFFFB900)
private val RemainingGray = Color(0xFFD5D5D5)
