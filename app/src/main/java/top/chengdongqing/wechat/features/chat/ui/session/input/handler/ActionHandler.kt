package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.util.CallOptions
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.util.createImageUri
import top.chengdongqing.wechat.core.util.createVideoUri
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.MoreAction

/**
 * 更多面板操作路由表
 */
class ActionHandler(
    private val onAlbum: (isLongClick: Boolean) -> Unit,
    private val onCamera: (isLongClick: Boolean) -> Unit,
    private val onVideoCall: () -> Unit,
    private val onLocation: () -> Unit,
    private val onFile: () -> Unit,
    private val onApk: () -> Unit,
    private val onCard: () -> Unit,
    private val onFavorite: () -> Unit,
    private val onVoiceInput: () -> Unit
) {
    /** 统一入口，按 action 路由 */
    fun handleAction(action: MoreAction, isLongClick: Boolean) {
        when (action) {
            MoreAction.Album -> onAlbum(isLongClick)
            MoreAction.Camera -> onCamera(isLongClick)
            MoreAction.VideoCall -> onVideoCall()
            MoreAction.Location -> onLocation()
            MoreAction.File -> onFile()
            MoreAction.Card -> onCard()
            MoreAction.Favorite -> onFavorite()
            MoreAction.Voice -> onVoiceInput()
            MoreAction.Apk -> onApk()
            else -> Unit
        }
    }
}

/**
 * 在 InputBarActionsFactory 中组装 ActionHandler
 */
@Composable
fun rememberActionHandler(
    mediaLaunchers: MediaLaunchers,
    locationLauncher: LocationLauncher,
    fileLauncher: FileLauncher,
    onSendMessage: (MessageContent) -> Unit,
    onLaunchCall: (CallType) -> Unit
): ActionHandler {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actionSheet = rememberActionSheetState()
    val chatContext = LocalChatSessionContext.current
    val isSelf = chatContext?.isSelf.isTrue()

    // 动态生成位置选项根据
    val locationOptions = remember(isSelf) {
        if (isSelf) listOf(LocationOptions[0]) else LocationOptions
    }

    /**
     * 启动系统相机
     */
    fun launchSystemCamera(isVideo: Boolean) {
        scope.launch {
            val uri = if (isVideo) {
                context.createVideoUri()
            } else {
                context.createImageUri()
            }
            mediaLaunchers.setCapturedUri(uri)

            if (isVideo) {
                mediaLaunchers.captureVideo.launch(uri)
            } else {
                mediaLaunchers.takePicture.launch(uri)
            }
        }
    }

    return remember(mediaLaunchers, locationLauncher, fileLauncher, isSelf) {
        ActionHandler(
            onAlbum = { isLongClick ->
                if (isLongClick) {
                    // 长按：打开系统图库
                    mediaLaunchers.launchSystemMediaPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                            9
                        )
                    )
                } else {
                    // 短按：打开内置图库
                    mediaLaunchers.launchMediaPicker(
                        VisualMediaType.ImageAndVideo,
                        9
                    )
                }
            },
            onCamera = { isLongClick ->
                if (isLongClick) {
                    // 长按：显示系统相机选项
                    actionSheet.show(
                        options = CameraOptions,
                        title = "调用系统相机"
                    ) { index ->
                        when (index) {
                            0 -> launchSystemCamera(false)
                            1 -> launchSystemCamera(true)
                        }
                    }
                } else {
                    // 短按：调用内置相机
                    mediaLaunchers.launchCamera(
                        VisualMediaType.ImageAndVideo
                    )
                }
            },
            onVideoCall = {
                actionSheet.show(CallOptions) { index ->
                    onLaunchCall(if (index == 0) CallType.Video else CallType.Voice)
                }
            },
            onLocation = {
                actionSheet.show(locationOptions) { index ->
                    when (index) {
                        0 -> locationLauncher.pickLocation()
                        1 -> {} // TODO: 实时位置
                    }
                }
            },
            onFile = fileLauncher.pickFile,
            onApk = fileLauncher.pickApk,
            onCard = {
                val content = MessageContent.ContactCard(
                    userId = randomUUID(),
                    name = "文件传输助手",
                    avatar = ""
                )
                onSendMessage(content)
            },
            onFavorite = { /* TODO */ },
            onVoiceInput = { /* TODO */ }
        )
    }
}

private val CameraOptions = listOf(
    ActionSheetItem("拍摄照片"),
    ActionSheetItem("拍摄视频")
)

val LocationOptions = listOf(
    ActionSheetItem("发送位置"),
    ActionSheetItem("共享实时位置")
)