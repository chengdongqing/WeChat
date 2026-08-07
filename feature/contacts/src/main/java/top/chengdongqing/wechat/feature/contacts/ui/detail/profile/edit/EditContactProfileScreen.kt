package top.chengdongqing.wechat.feature.contacts.ui.detail.profile.edit

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastManager
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.LinkBlue
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun EditContactProfileScreen(
    contactId: String,
    onBack: () -> Unit,
    onManageTags: () -> Unit = {},
    viewModel: EditContactProfileViewModel = hiltViewModel { factory: EditContactProfileViewModel.Factory ->
        factory.create(contactId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contact = uiState.contact

    // 监听保存事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditProfileEvent.SaveSuccess -> {
                    onBack()
                }

                is EditProfileEvent.SaveError -> {
                    ToastManager.fail(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            WeTopAppBar(
                onBack = onBack,
                backText = stringResource(R.string.action_cancel),
                containerColor = WeTheme.colorScheme.surface
            ) {
                WeButton(
                    text = stringResource(R.string.action_done),
                    size = ButtonSize.Small,
                    loading = uiState.isSaving
                ) {
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
                    label = stringResource(R.string.contact_profile_edit_label_name),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    WeInput(
                        value = uiState.remarkName,
                        placeholder = contact?.nickname ?: "",
                        maxLength = 17,
                        showDivider = false,
                        onValueChange = { viewModel.updateRemarkName(it) }
                    )
                }
                ListItem(label = stringResource(R.string.contact_profile_edit_label_tags)) {
                    LinkedRow(
                        label = stringResource(R.string.contact_profile_edit_add_tag),
                        onClick = onManageTags
                    )
                }
                ListItem(
                    label = stringResource(R.string.contact_profile_edit_label_note),
                    padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    WeInput(
                        value = uiState.note,
                        placeholder = stringResource(R.string.contact_profile_edit_note_placeholder),
                        singleLine = false,
                        maxLength = 100,
                        showDivider = false,
                        onValueChange = { viewModel.updateNote(it) }
                    )
                }
                ListItem(
                    label = stringResource(R.string.contact_profile_edit_label_photo),
                    modifier = Modifier.size(120.dp)
                ) {
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
            text = stringResource(R.string.contact_profile_edit_title),
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
private fun LinkedRow(
    label: String = stringResource(R.string.contact_profile_edit_add_tag),
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onTap { onClick() }
    ) {
        Text(text = label, color = LinkBlue, fontSize = 16.sp)
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
            tint = LinkBlue,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.contact_profile_edit_add_photo),
            color = LinkBlue,
            fontSize = 15.sp
        )
    }
}
