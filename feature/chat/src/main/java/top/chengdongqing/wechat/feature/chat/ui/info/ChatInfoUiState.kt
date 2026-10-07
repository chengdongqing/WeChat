package top.chengdongqing.wechat.feature.chat.ui.info

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.feature.chat.ai.LocalAiModelInfo
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState

@Immutable
data class ChatInfoUiState(
    val contact: Contact? = null,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isBottomed: Boolean = false,
    val backgroundPath: String? = null,
    val isTemporary: Boolean = false,
    val expiresAt: Long? = null,
    val localAiState: LocalAiState = LocalAiState.NoModel,
    val modelSizeBytes: Long? = null,
    val modelInfo: LocalAiModelInfo? = null
)