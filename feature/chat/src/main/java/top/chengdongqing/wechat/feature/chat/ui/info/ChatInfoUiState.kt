package top.chengdongqing.wechat.feature.chat.ui.info

import top.chengdongqing.wechat.feature.chat.ai.LocalAiModelInfo
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState

data class ChatInfoUiState(
    /** 联系人信息 */
    val contactName: String = "",
    val contactAvatar: String? = null,

    /** 会话设置 */
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isBottomed: Boolean = false,
    val backgroundPath: String? = null,
    val isTemporary: Boolean = false,
    val expiresAt: Long? = null,
    val isFriend: Boolean = false,
    val isAiAssistant: Boolean = false,
    val localAiState: LocalAiState = LocalAiState.NoModel,
    val modelSizeBytes: Long? = null,
    val modelInfo: LocalAiModelInfo? = null
)