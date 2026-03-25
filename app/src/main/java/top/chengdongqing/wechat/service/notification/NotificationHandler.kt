package top.chengdongqing.wechat.service.notification

import android.content.Context
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.common.media.VibratorHelper
import top.chengdongqing.wechat.core.common.notification.NotificationDisplay
import top.chengdongqing.wechat.core.common.notification.NotificationSound
import top.chengdongqing.wechat.core.common.notification.toUri
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.FriendEvent
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.core.data.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.designsystem.ui.toPreviewText
import top.chengdongqing.wechat.core.network.messaging.MessageDispatcher
import top.chengdongqing.wechat.core.network.service.addfriend.BLEAddFriendHandler
import top.chengdongqing.wechat.core.network.service.notification.NotificationServiceModule
import top.chengdongqing.wechat.core.network.session.ActiveSessionManager
import top.chengdongqing.wechat.feature.chat.data.mapper.toMessageType
import javax.inject.Inject
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Singleton
class NotificationHandler @Inject constructor(
    private val messageDispatcher: MessageDispatcher,
    private val addFriendModule: BLEAddFriendHandler,
    private val friendRequestRepository: FriendRequestRepository,
    private val notificationHelper: NotificationHelper,
    private val vibratorHelper: VibratorHelper,
    private val contactRepository: ContactRepository,
    private val chatSessionDao: ChatSessionDao,
    private val notificationRepository: NotificationSettingsRepository,
    private val activeSessionManager: ActiveSessionManager,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) : NotificationServiceModule {
    private companion object {
        const val TAG = "NotificationHandler"
    }

    private var observerFriendJob: Job? = null
    private var observerMessageJob: Job? = null

    override fun start() {
        runCatching {
            // 监听加好友事件
            observerFriendJob = scope.launch {
                observeFriendEvents()
            }
            // 监听新消息
            observerMessageJob = scope.launch {
                observeIncomingMessages()
            }
        }.onSuccess {
            Log.d(TAG, "通知模块已启动")
        }
    }

    override fun stop() {
        runCatching {
            // 取消订阅状态
            observerFriendJob?.cancel()
            observerMessageJob?.cancel()
        }.onSuccess {
            Log.d(TAG, "通知模块已停止")
        }
    }

    /**
     * 监听加好友相关事件
     */
    private suspend fun observeFriendEvents() {
        merge(
            addFriendModule.friendEvents,
            friendRequestRepository.friendEvents
        ).collect { event ->
            handleFriendNotification(event)
        }
    }

    /**
     * 监听新消息
     */
    private suspend fun observeIncomingMessages() {
        messageDispatcher.incomingMessages.collect { message ->
            handleMessageNotification(message)
        }
    }

    /**
     * 显示加好友相关事件通知
     */
    private fun handleFriendNotification(event: FriendEvent) {
        when (event) {
            is FriendEvent.FriendRequest -> notificationHelper.showFriendNotification(
                title = event.nickname,
                content = context.getString(DesignR.string.contact_notification_request_content),
            )

            is FriendEvent.Added -> notificationHelper.showFriendNotification(
                title = context.getString(DesignR.string.contact_notification_auto_added_title),
                content = context.getString(
                    DesignR.string.contact_notification_auto_added_content,
                    event.nickname
                ),
                contactId = event.contactId
            )
        }
    }

    /**
     * 显示新消息通知
     */
    private suspend fun handleMessageNotification(message: ChatMessage) {
        // 获取通知设置
        val soundEnabled = inChatSoundEnabled() // 开启声音
        val vibrationEnabled = inChatVibrationEnabled() // 开启振动

        if (soundEnabled && vibrationEnabled) {
            val contact = contactRepository.getContact(message.senderId)
            // 联系人名字
            val sender = contact?.displayName
                ?: context.getString(DesignR.string.chat_notification_contact_unknown)
            // 未读数
            val unreadCount = chatSessionDao.getById(message.senderId)?.unreadCount ?: 0
            // 消息内容
            val content = resolveContent(message)

            // 构建通知内容
            val (title, text) = resolveNotificationText(sender, content, unreadCount)
            // 获取头像
            val avatarBitmap = contact?.avatarPath?.let { BitmapFactory.decodeFile(it) }

            // 显示通知
            notificationHelper.showMessageNotification(
                sessionId = message.sessionId,
                title = title,
                content = text,
                notificationId = message.sessionId.hashCode(),
                avatarBitmap = avatarBitmap
            )
        }

        // 播放通知提示音
        if (soundEnabled) {
            val soundUri = notificationSound().toUri(context)
            RingtoneManager.getRingtone(context, soundUri).play()
        }
        // 触发振动
        if (vibrationEnabled) {
            vibratorHelper.vibrate(longArrayOf(0, 250, 250, 250))
        }
    }

    private fun resolveContent(message: ChatMessage): String {
        val textContent = (message.content as? MessageContent.Text)?.text ?: ""
        return message.content.toMessageType().toPreviewText(context, textContent)
    }

    private suspend fun resolveNotificationText(
        sender: String,
        content: String,
        unreadCount: Int
    ): Pair<String?, String> {
        val prefix = if (unreadCount > 1) "[${unreadCount}条] " else ""
        return when (notificationDisplay()) {
            NotificationDisplay.HiddenAll -> Pair(
                null,
                prefix + context.getString(DesignR.string.chat_notification_hidden)
            )

            NotificationDisplay.SenderOnly -> Pair(
                sender,
                prefix + context.getString(DesignR.string.chat_notification_sender_only)
            )

            NotificationDisplay.SenderAndContent -> Pair(
                sender,
                prefix + content
            )
        }
    }

    private suspend fun notificationDisplay(): NotificationDisplay =
        notificationRepository.notificationDisplay.first()

    private suspend fun inChatSoundEnabled(): Boolean =
        !activeSessionManager.inChat || notificationRepository.inChatSoundEnabled.first()

    private suspend fun inChatVibrationEnabled(): Boolean =
        !activeSessionManager.inChat || notificationRepository.inChatVibrationEnabled.first()

    private suspend fun notificationSound(): NotificationSound =
        notificationRepository.notificationSound.first()
}