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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.dialog.DialogManager
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
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.settings_storage),
                onBack = onBack
            )
        },
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
    val cacheLabel = stringResource(R.string.storage_cache)
    val chatMediaLabel = stringResource(R.string.storage_chat_media)
    val resourcesLabel = stringResource(R.string.storage_resources)
    val cleanConfirmation = stringResource(R.string.storage_clean_confirmation)
    val colors = storageColors()

    fun handleClean(category: StorageCategory) {
        val label = when (category) {
            StorageCategory.Cache -> cacheLabel
            StorageCategory.Chats -> chatMediaLabel
            StorageCategory.Resources -> resourcesLabel
        }

        DialogManager.show(
            title = cleanConfirmation.format(label),
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
            StorageBar(state, colors)
            Spacer(Modifier.height(6.dp))
            StorageLegend(colors)
        }
        CurrentUsedStorage(state)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StorageCard(
                title = stringResource(R.string.storage_cache),
                bytes = state.cacheBytes,
                description = stringResource(R.string.storage_cache_desc),
                category = StorageCategory.Cache,
                cleaning = state.cleaning,
                emphasized = true,
                onClean = ::handleClean
            )
            StorageCard(
                title = stringResource(R.string.storage_chats),
                bytes = state.chatBytes,
                description = stringResource(R.string.storage_chats_desc),
                category = StorageCategory.Chats,
                cleaning = state.cleaning,
                onClean = ::handleClean
            )
            StorageCard(
                title = stringResource(R.string.storage_resources),
                bytes = state.resourceBytes,
                description = stringResource(R.string.storage_resources_desc),
                category = StorageCategory.Resources,
                cleaning = state.cleaning,
                onClean = ::handleClean
            )
            StorageCard(
                title = stringResource(R.string.storage_essential_files),
                bytes = state.necessaryBytes,
                description = stringResource(R.string.storage_essential_files_desc),
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
            state.totalBytes == 0L -> 0f
            else -> (state.appBytes.toFloat() * 100 / state.totalBytes).coerceIn(0f, 100f)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.storage_wechat_used_space),
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
            text = stringResource(R.string.storage_device_percentage, percent),
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun StorageBar(
    state: StorageUiState,
    colors: StorageColors
) {
    val total = state.totalBytes.coerceAtLeast(1)
    val appFraction = (state.appBytes.toFloat() / total).coerceIn(0f, 1f)
    val deviceUsed = (total - state.freeBytes).coerceAtLeast(state.appBytes)
    val otherFraction = ((deviceUsed - state.appBytes).toFloat() / total)
        .coerceIn(0f, 1f - appFraction)
    val remainingFraction = (1f - appFraction - otherFraction)

    val chartItems = listOf(
        Pair(appFraction, colors.wechat),
        Pair(otherFraction, colors.otherApps),
        Pair(remainingFraction, colors.available)
    )

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
private fun StorageLegend(
    colors: StorageColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(colors.wechat, stringResource(R.string.storage_legend_wechat))
        LegendItem(colors.otherApps, stringResource(R.string.storage_legend_other_apps))
        LegendItem(colors.available, stringResource(R.string.storage_legend_available))
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
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
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (category != null) {
            WeButton(
                text = stringResource(R.string.storage_clean),
                size = ButtonSize.Small,
                type = if (emphasized) ButtonType.Primary else ButtonType.Plain,
                loading = cleaning == category,
                enabled = bytes > 0,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                onClean(category)
            }
        }
    }
}

private data class StorageColors(
    val wechat: Color,
    val otherApps: Color,
    val available: Color
)

@Composable
private fun storageColors() = StorageColors(
    wechat = WeTheme.colorScheme.primary,
    otherApps = Color(0xFFFFB900),
    available = Color(0xFFD5D5D5)
)
