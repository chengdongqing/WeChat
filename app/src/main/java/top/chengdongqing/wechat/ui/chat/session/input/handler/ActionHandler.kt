package top.chengdongqing.wechat.ui.chat.session.input.handler

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.createMediaUri
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.call.model.CallType
import top.chengdongqing.wechat.data.model.CallStatus
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.ui.call.startCall
import top.chengdongqing.wechat.ui.chat.session.input.panel.MoreAction
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetState
import top.chengdongqing.wechat.ui.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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
    private val onSendMessage: (MessageContent, (() -> Unit)?) -> Unit
) {
    fun onSendMessage(content: MessageContent) = onSendMessage(content, null)

    /**
     * 处理动作
     */
    fun handleAction(action: MoreAction, isLongClick: Boolean) {
        when (action) {
            MoreAction.Album -> handleAlbum()
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
    private fun handleAlbum() {
        mediaLaunchers.mediaPicker(VisualMediaType.ImageAndVideo, 9)
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
            mediaLaunchers.camera(VisualMediaType.ImageAndVideo)
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
     * 处理视频通话
     */
    private fun handleVideoCall() {
        actionSheet.show(CallOptions) { index ->
            // 发送通话消息
            val content = MessageContent.Call(
                type = if (index == 0) CallType.Video else CallType.Voice,
                status = CallStatus.Connected,
                duration = (3.minutes + 26.seconds).toLong(DurationUnit.MILLISECONDS)
            )
            onSendMessage(content)

            // 启动通话界面
            context.startCall(
                callType = if (index == 0) CallType.Video else CallType.Voice,
                userId = randomUUID(),
                userName = "海盐芝士不加糖"
            )
        }
    }

    /**
     * 处理位置
     */
    private fun handleLocation() {
        actionSheet.show(LocationOptions) { index ->
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
        val content = MessageContent.File(
            fileName = "extra_data.b",
            fileSize = (6.9 * 1024 * 1024).toLong(),
            fileType = "file",
            fileUrl = ""
        )
        onSendMessage(content)
    }

    /**
     * 处理名片
     */
    private fun handleCard() {
        val content = MessageContent.UserCard(
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
         * 通话选项
         */
        val CallOptions = listOf(
            ActionSheetItem("视频通话", icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_video_call_filled),
                    contentDescription = null,
                    tint = WeChatTheme.colorScheme.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }),
            ActionSheetItem("语音通话", icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_voice_call_filled),
                    contentDescription = null,
                    tint = WeChatTheme.colorScheme.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            })
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
    context: Context,
    mediaHandler: MediaHandler,
    locationHandler: LocationHandler,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit
): ActionHandler {
    val scope = rememberCoroutineScope()
    val mediaLaunchers = rememberMediaLaunchers(mediaHandler)
    val locationLauncher = rememberLocationLauncher(locationHandler)
    val actionSheet = rememberActionSheetState()

    return remember(context, mediaLaunchers) {
        ActionHandler(
            context = context,
            scope = scope,
            mediaLaunchers = mediaLaunchers,
            actionSheet = actionSheet,
            locationLauncher = locationLauncher,
            onSendMessage = onSendMessage
        )
    }
}