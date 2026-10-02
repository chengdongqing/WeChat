package top.chengdongqing.wechat.feature.auth.ui

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class LoginUiState(
    val userName: String = "",
    val avatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)