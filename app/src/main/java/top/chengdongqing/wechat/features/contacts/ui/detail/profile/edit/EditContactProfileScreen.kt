package top.chengdongqing.wechat.features.contacts.ui.detail.profile.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.LinkColor
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable

@Composable
fun EditContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: EditContactProfileViewModel = hiltViewModel { factory: EditContactProfileViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contact = uiState.contact ?: return
    val toast = rememberToastState()

    // 监听保存事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditProfileEvent.SaveSuccess -> {
                    onBack()
                }

                is EditProfileEvent.SaveError -> {
                    toast.show(title = event.message, icon = ToastIcon.Fail)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            WeTopBar(
                onBack = onBack,
                backText = "取消",
                containerColor = WeTheme.colorScheme.surface
            ) {
                WeButton(text = "完成", size = ButtonSize.Small, loading = uiState.isSaving) {
                    viewModel.saveChanges()
                }
            }
        },
        containerColor = WeTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleSection()
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                ListItem(
                    label = "备注名",
                    padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    WeInput(
                        value = uiState.remarkName,
                        placeholder = contact.nickname,
                        maxLength = 17,
                        showDivider = false,
                        onValueChange = { viewModel.updateRemarkName(it) }
                    )
                }
                ListItem("标签") {
                    LinkedRow("添加标签") {}
                }
                ListItem(
                    label = "备忘",
                    padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    WeInput(
                        value = uiState.note,
                        placeholder = "添加文字",
                        singleLine = false,
                        maxLength = 100,
                        showDivider = false,
                        onValueChange = { viewModel.updateNote(it) }
                    )
                }
                ListItem(label = "照片", modifier = Modifier.size(120.dp)) {
                    PhotoSection()
                }
            }
        }
    }
}

@Composable
private fun TitleSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "设置备注",
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ListItem(
    label: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WeTheme.colorScheme.surfaceVariant)
                .padding(padding)
        ) {
            content()
        }
    }
}

@Composable
private fun LinkedRow(label: String, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weClickable { onClick() }
    ) {
        Text(text = label, color = LinkColor, fontSize = 16.sp)
        Icon(
            painter = painterResource(R.drawable.ic_right_outlined),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PhotoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(WeTheme.colorScheme.surfaceVariant)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus_circle_outlined),
            contentDescription = null,
            tint = LinkColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "添加照片", color = LinkColor, fontSize = 15.sp)
    }
}