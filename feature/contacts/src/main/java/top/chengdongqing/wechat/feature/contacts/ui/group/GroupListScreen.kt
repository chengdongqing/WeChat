package top.chengdongqing.wechat.feature.contacts.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import javax.inject.Inject

@Composable
fun GroupListScreen(
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(WeTheme.colorScheme.background)) {
        WeTopAppBar(title = "群聊", onBack = onBack)
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无保存的群聊", color = WeTheme.colorScheme.textSecondary)
            }
        } else {
            LazyColumn {
                items(groups, key = { it.id }) { group ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(WeTheme.colorScheme.surface)
                            .clickable { onOpenGroup(group.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = group.avatarPath,
                            error = painterResource(R.drawable.img_avatar_placeholder),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(5.dp))
                        )
                        Text(
                            group.remark?.takeIf(String::isNotBlank) ?: group.name,
                            fontSize = 16.sp,
                            color = WeTheme.colorScheme.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WeDivider(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@HiltViewModel
class GroupListViewModel @Inject constructor(groupDao: GroupDao) : ViewModel() {
    val groups = groupDao.observeSavedGroups().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
}
