package top.chengdongqing.wechat.features.me.ui.profile.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.radio.WeRadioGroup
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.ui.profile.ProfileEventEffect
import top.chengdongqing.wechat.features.me.ui.profile.ProfileField
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel

@Composable
fun EditGenderScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialGender = uiState.profile?.gender
    val genderOptions = remember {
        Gender.entries.map { resources.getString(it.label) to it }
    }
    var gender by remember(initialGender) { mutableStateOf(initialGender) }
    val hasChanged = gender != initialGender

    ProfileEventEffect(viewModel, onBack)

    Scaffold(
        topBar = {
            WeTopBar(
                title = stringResource(R.string.me_edit_gender),
                onBack = onBack
            ) {
                WeButton(
                    text = stringResource(R.string.action_done),
                    size = ButtonSize.Small,
                    enabled = hasChanged
                ) {
                    viewModel.updateField(ProfileField.Gender(gender!!))
                }
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            WeRadioGroup(
                options = genderOptions,
                value = gender
            ) {
                gender = it
            }
        }
    }
}