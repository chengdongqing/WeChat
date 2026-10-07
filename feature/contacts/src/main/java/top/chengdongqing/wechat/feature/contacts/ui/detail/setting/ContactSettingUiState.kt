package top.chengdongqing.wechat.feature.contacts.ui.detail.setting

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.model.Contact

@Immutable
data class ContactSettingUiState(
    val contact: Contact? = null
)
