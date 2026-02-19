package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.util.CallOptions
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.util.createMediaUri
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatContext
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.MoreAction

/**
 * 动作处理器
 *
 * 负责处理"更多"面板中的各种操作
 */
class ActionHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val mediaLaunchers: MediaLaunchers,
    private val actionSheet: ActionSheetState,
    private val locationLauncher: LocationLauncher,
    private val fileLauncher: FileLauncher,
    private val onSendMessage: (MessageContent, (() -> Unit)?) -> Unit,
    private val onLaunchCall: (type: CallType) -> Unit,
    private val isMyself: Boolean
) {
    fun onSendMessage(content: MessageContent) = onSendMessage(content, null)

    /**
     * 处理动作
     */
    fun handleAction(action: MoreAction, isLongClick: Boolean) {
        when (action) {
            MoreAction.Album -> handleAlbum(isLongClick)
            MoreAction.Camera -> handleCamera(isLongClick)
            MoreAction.VideoCall -> handleVideoCall()
            MoreAction.Location -> handleLocation()
            MoreAction.File -> handleFile()
            MoreAction.Card -> handleCard()
            MoreAction.Favorite -> handleFavorite()
            MoreAction.Voice -> handleVoice()
            else -> {}
        }
    }

    /**
     * 处理相册
     */
    private fun handleAlbum(isLongClick: Boolean, maxItems: Int = 9) {
        if (isLongClick) {
            // 长按：打开系统图库
            scope.launch {
                mediaLaunchers.launchSystemMediaPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                        maxItems
                    )
                )
            }
        } else {
            // 短按：打开内置图库
            mediaLaunchers.launchMediaPicker(VisualMediaType.ImageAndVideo, maxItems)
        }
    }

    /**
     * 处理拍摄
     */
    private fun handleCamera(isLongClick: Boolean) {
        if (isLongClick) {
            // 长按：显示系统相机选项
            actionSheet.show(
                options = TakeMediaOptions,
                title = "调用系统相机"
            ) { index ->
                when (index) {
                    0 -> launchSystemCamera(isVideo = false)
                    1 -> launchSystemCamera(isVideo = true)
                }
            }
        } else {
            // 短按：打开内置相机
            mediaLaunchers.launchCamera(VisualMediaType.ImageAndVideo)
        }
    }

    /**
     * 启动系统相机
     */
    private fun launchSystemCamera(isVideo: Boolean) {
        scope.launch {
            val uri = context.createMediaUri(isVideo)
            mediaLaunchers.setCapturedUri(uri)

            if (isVideo) {
                mediaLaunchers.captureVideo.launch(uri)
            } else {
                mediaLaunchers.takePicture.launch(uri)
            }
        }
    }

    /**
     * 处理视频/语音通话
     */
    private fun handleVideoCall() {
        actionSheet.show(CallOptions) { index ->
            val callType = when (index) {
                0 -> CallType.Video
                else -> CallType.Voice
            }
            onLaunchCall(callType)
        }
    }

    private val currentLocationOptions = if (isMyself) {
        listOf(LocationOptions[0])
    } else {
        LocationOptions
    }

    /**
     * 处理位置
     */
    private fun handleLocation() {
        actionSheet.show(currentLocationOptions) { index ->
            when (index) {
                0 -> locationLauncher.pickLocation()
                1 -> handleShareLiveLocation()
            }
        }
    }

    /**
     * 处理共享实时位置
     */
    private fun handleShareLiveLocation() {
        // TODO: 实现共享实时位置
    }

    /**
     * 处理文件
     */
    private fun handleFile() {
        fileLauncher.pickFile()
    }

    /**
     * 处理名片
     */
    private fun handleCard() {
        val content = MessageContent.ContactCard(
            userId = randomUUID(),
            name = "文件传输助手",
            avatar = ""
        )
        onSendMessage(content)
    }

    /**
     * 处理收藏
     */
    private fun handleFavorite() {
        // TODO: 实现收藏功能
    }

    /**
     * 处理语音输入
     */
    private fun handleVoice() {
        // TODO: 实现语音输入功能
    }

    companion object {
        /**
         * 拍摄选项
         */
        val TakeMediaOptions = listOf(
            ActionSheetItem("拍摄照片"),
            ActionSheetItem("拍摄视频")
        )

        /**
         * 位置选项
         */
        val LocationOptions = listOf(
            ActionSheetItem("发送位置"),
            ActionSheetItem("共享实时位置")
        )
    }
}

@Composable
fun rememberActionHandler(
    mediaHandler: MediaHandler,
    locationHandler: LocationHandler,
    fileHandler: FileHandler,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit,
    onLaunchCall: (type: CallType) -> Unit
): ActionHandler {
    val scope = rememberCoroutineScope()
    val mediaLaunchers = rememberMediaLaunchers(mediaHandler)
    val locationLauncher = rememberLocationLauncher(locationHandler)
    val fileLauncher = rememberFileLauncher(fileHandler)
    val actionSheet = rememberActionSheetState()

    val context = LocalContext.current
    val chatContext = LocalChatContext.current
    val isMyself = chatContext?.isMyself.isTrue()

    return remember(context, mediaLaunchers, isMyself) {
        ActionHandler(
            context = context,
            scope = scope,
            mediaLaunchers = mediaLaunchers,
            actionSheet = actionSheet,
            locationLauncher = locationLauncher,
            fileLauncher = fileLauncher,
            onSendMessage = onSendMessage,
            onLaunchCall = onLaunchCall,
            isMyself = isMyself
        )
    }
}