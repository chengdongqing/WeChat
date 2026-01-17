package top.chengdongqing.wechat.ui.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.data.model.P2PPeer
import top.chengdongqing.wechat.data.model.WifiLanPeer
import top.chengdongqing.wechat.ui.chat.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Discovery1Screen(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit
) {
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("发现附近的设备") })
        },
        floatingActionButton = {
            // 扫描按钮
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.toggleDiscovery()
                },
                icon = {
                    Icon(
                        if (isDiscovering) Icons.Default.Stop else Icons.Default.Search,
                        null
                    )
                },
                text = {
                    Text(if (isDiscovering) "停止扫描" else "开始扫描")
                },
                containerColor = if (isDiscovering) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
        }
    ) { padding ->
        if (peers.isEmpty()) {
            EmptyDiscoveryView(isDiscovering, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(peers, key = { it.id }) { peer ->
                    PeerItem(
                        peer = peer,
                        onClick = {
                            // 停止扫描
                            viewModel.stopDiscovery()
                            // 连接设备
                            viewModel.connectToPeer(peer)
                            // 跳转到聊天页面
                            onNavigateToChat(peer.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PeerItem(peer: P2PPeer, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(peer.name) },
        supportingContent = {
            // 如果是 LanPeer，可以显示 IP 地址；如果是蓝牙，可以显示信号强度
            val info = if (peer is WifiLanPeer) peer.ip else "附近的设备"
            Text(info)
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}

@Composable
fun EmptyDiscoveryView(
    isDiscovering: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isDiscovering) {
            // 正在搜索时的状态
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在雷达扫描中...", style = MaterialTheme.typography.bodyLarge)
            Text("请确保对方也开启了“开始发现”", style = MaterialTheme.typography.bodySmall)
        } else {
            // 未开启搜索时的状态
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("雷达未开启", style = MaterialTheme.typography.bodyLarge)
            Text("点击下方按钮搜索附近的朋友", style = MaterialTheme.typography.bodySmall)
        }
    }
}