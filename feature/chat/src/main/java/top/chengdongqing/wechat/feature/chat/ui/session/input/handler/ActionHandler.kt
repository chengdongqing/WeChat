package top.chengdongqing.wechat.feature.chat.ui.session.input.handler

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.call.ui.CallOptions
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetManager
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.file.copyResourceToUri
import top.chengdongqing.wechat.core.file.createImageUri
import top.chengdongqing.wechat.core.file.createVideoUri
import top.chengdongqing.wechat.core.file.deleteFileByUri
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.media.picker.MediaPickerRequest
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext
import top.chengdongqing.wechat.feature.chat.ui.session.input.panel.MoreAction
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.chat.R as ChatR

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
    private val onContactCard: () -> Unit,
    private val onMusic: () -> Unit,
    private val onLive: () -> Unit,
    private val onTransfer: () -> Unit,
    private val onFavorite: () -> Unit
) {
    /** 统一入口，按 action 路由 */
    fun handleAction(action: MoreAction, isLongClick: Boolean) {
        when (action) {
            MoreAction.Album -> onAlbum(isLongClick)
            MoreAction.Camera -> onCamera(isLongClick)
            MoreAction.VideoCall -> onVideoCall()
            MoreAction.Location -> onLocation()
            MoreAction.File -> onFile()
            MoreAction.ContactCard -> onContactCard()
            MoreAction.App -> onApk()
            MoreAction.Music -> onMusic()
            MoreAction.Live -> onLive()
            MoreAction.Transfer -> onTransfer()
            MoreAction.Favorite -> onFavorite()
            else -> Unit
        }
    }
}

/**
 * 在 InputBarActionsProvider 中组装 ActionHandler
 */
@Composable
fun rememberActionHandler(
    mediaLaunchers: MediaLaunchers,
    fileLauncher: FileLauncher,
    onOpenFilePicker: () -> Unit,
    pickContact: () -> Unit,
    privateFileManager: PrivateFileManager,
    onPickLocation: () -> Unit,
    onShareLiveLocation: () -> Unit,
    onLaunchCall: (CallType) -> Unit,
    onSelectMusic: () -> Unit,
    onStartLive: () -> Unit,
    onOpenFavorites: () -> Unit,
    onSendMessage: (MessageContent) -> Unit
): ActionHandler {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val chatContext = LocalChatSessionContext.current
    val isSelf = chatContext?.isSelf == true

    // 动态生成位置选项
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

    return remember(mediaLaunchers, fileLauncher, isSelf) {
        ActionHandler(
            onAlbum = { isLongClick ->
                if (isLongClick) {
                    // 长按：打开系统图库
                    mediaLaunchers.launchSystemMediaPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                            99
                        )
                    )
                } else {
                    // 短按：打开内置图库
                    mediaLaunchers.launchMediaPicker.launch(
                        MediaPickerRequest(
                            mediaType = VisualMediaType.ImageAndVideo,
                            maxSelection = 99,
                            enableMerge = true
                        )
                    )
                }
            },
            onCamera = { isLongClick ->
                if (isLongClick) {
                    // 长按：显示系统相机选项
                    ActionSheetManager.show(
                        options = CameraOptions,
                        title = R.string.chat_camera_title
                    ) { index ->
                        when (index) {
                            0 -> launchSystemCamera(false)
                            1 -> launchSystemCamera(true)
                        }
                    }
                } else {
                    // 短按：调用内置相机
                    mediaLaunchers.launchCamera.launch(
                        VisualMediaType.ImageAndVideo
                    )
                }
            },
            onVideoCall = {
                ActionSheetManager.show(CallOptions) { index ->
                    onLaunchCall(if (index == 0) CallType.Video else CallType.Voice)
                }
            },
            onLocation = {
                ActionSheetManager.show(locationOptions) { index ->
                    when (index) {
                        0 -> onPickLocation()
                        1 -> onShareLiveLocation()
                    }
                }
            },
            onFile = onOpenFilePicker,
            onApk = fileLauncher.pickApk,
            onMusic = onSelectMusic,
            onLive = onStartLive,
            onContactCard = pickContact,
            onTransfer = {
                onSendMessage(
                    MessageContent.Text(
                        resources.getString(
                            ChatR.string.donate_description,
                            resources.getString(DesignR.string.app_name)
                        )
                    )
                )

                scope.launch {
                    delay(500.milliseconds)

                    val tempFile = File.createTempFile("Dotation_", ".jpg")
                    // 获取表情URI
                    val uri = context.copyResourceToUri(
                        resId = ChatR.drawable.img_donation,
                        targetFile = tempFile
                    ) ?: return@launch
                    // 拷贝到私有目录持久化保存
                    val localPath = privateFileManager.saveMedia(
                        messageType = MessageType.Sticker,
                        sourceUri = uri
                    ).getOrThrow()
                    // 清理临时文件
                    context.deleteFileByUri(uri)

                    onSendMessage(MessageContent.Sticker(localPath))
                }
            },
            onFavorite = onOpenFavorites
        )
    }
}

private val CameraOptions by lazy {
    listOf(
        ActionSheetItem(R.string.chat_camera_photo),
        ActionSheetItem(R.string.chat_camera_video)
    )
}

private val LocationOptions by lazy {
    listOf(
        ActionSheetItem(R.string.chat_location_send),
        ActionSheetItem(R.string.chat_location_share)
    )
}
