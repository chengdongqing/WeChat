package top.chengdongqing.wechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import top.chengdongqing.wechat.core.util.GenericViewModelFactory
import top.chengdongqing.wechat.data.local.DatabaseModule
import top.chengdongqing.wechat.data.model.P2pMode
import top.chengdongqing.wechat.data.network.BluetoothManager
import top.chengdongqing.wechat.data.repository.ChatRepositoryImpl
import top.chengdongqing.wechat.ui.chat.ChatViewModel
import top.chengdongqing.wechat.ui.components.PermissionWrapper
import top.chengdongqing.wechat.ui.navigation.AppNavigation
import top.chengdongqing.wechat.ui.theme.WeChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeChatTheme {
                PermissionWrapper(mode = P2pMode.BLUETOOTH) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private val viewModel: ChatViewModel by viewModels {
        GenericViewModelFactory {
            val db = DatabaseModule.getDatabase(applicationContext)
//            val connectionManager = WifiLanManager(applicationContext)
            val connectionManager = BluetoothManager(applicationContext)
            val repository = ChatRepositoryImpl(db.messageDao(), connectionManager)

            ChatViewModel(repository, connectionManager, application)
        }
    }
}
