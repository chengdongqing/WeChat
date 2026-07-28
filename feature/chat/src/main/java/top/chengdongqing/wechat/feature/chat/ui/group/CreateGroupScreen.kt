package top.chengdongqing.wechat.feature.chat.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.navigation.LocalContactPickerLauncher
import top.chengdongqing.wechat.core.network.messaging.GroupChatCoordinator
import javax.inject.Inject

@Composable
fun CreateGroupScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickContacts = LocalContactPickerLauncher.current.rememberLauncher(excludeSelf = true) { contacts ->
        if (contacts.isEmpty()) onBack() else viewModel.create(contacts)
    }
    LaunchedEffect(Unit) { pickContacts(200) }
    LaunchedEffect(state.groupId) { state.groupId?.let(onCreated) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.error == null) CircularProgressIndicator()
        Text(
            text = state.error ?: "选择群成员后将通过局域网同步群聊",
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val coordinator: GroupChatCoordinator
) : ViewModel() {
    private val _state = MutableStateFlow(CreateGroupState())
    val state = _state.asStateFlow()

    fun create(contacts: Array<ContactResult>) {
        viewModelScope.launch {
            coordinator.createGroup(contacts.toList())
                .onSuccess { _state.value = CreateGroupState(groupId = it) }
                .onFailure { _state.value = CreateGroupState(error = it.message ?: "建群失败") }
        }
    }
}

data class CreateGroupState(val groupId: String? = null, val error: String? = null)
