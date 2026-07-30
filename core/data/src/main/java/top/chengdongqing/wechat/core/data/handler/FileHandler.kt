package top.chengdongqing.wechat.core.data.handler

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.common.app.model.AppResult
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.common.file.getFileMetadata
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.model.MessageType
import java.io.File

/**
 * 文件处理器
 */
class FileHandler(
    private val privateFileManager: PrivateFileManager,
    private val onSendMessage: (MessageContent) -> Unit
) {
    /**
     * 处理文件选择结果
     */
    suspend fun handleFileSelection(uris: List<Uri>, context: Context) {
        uris.forEachIndexed { index, uri ->
            // 解析元数据
            val metadata = context.getFileMetadata(uri) ?: return

            // 拷贝到私有目录
            val localPath = privateFileManager.saveMedia(
                messageType = MessageType.File,
                sourceUri = uri
            ).getOrThrow()

            // 构建消息内容
            val content = MessageContent.File(
                localPath = localPath,
                mimeType = metadata.mimeType,
                filename = metadata.filename,
                size = metadata.size
            )

            // 发送
            onSendMessage(content)

            if (index < uris.lastIndex) delay(50)
        }
    }

    /**
     * 处理 App 选择结果
     */
    suspend fun handleAppSelection(apps: Array<AppResult>) {
        apps.forEachIndexed { index, app ->
            // 构建消息内容
            val content = MessageContent.File(
                localPath = app.filePath,
                mimeType = app.mimeType,
                filename = app.fileName,
                size = app.fileSize
            )

            // 发送
            onSendMessage(content)

            if (index < apps.lastIndex) delay(50)
        }
    }

    /**
     * 处理名片选择结果
     */
    suspend fun handleContactSelection(contact: ContactResult) {
        // 拷贝到私有目录
        val localPath = privateFileManager.saveMedia(
            messageType = MessageType.ContactCard,
            sourceFile = File(contact.avatarPath)
        ).getOrThrow()

        // 构建消息内容
        val content = MessageContent.ContactCard(
            userId = contact.id,
            nickname = contact.nickname,
            avatarPath = localPath
        )

        // 发送
        onSendMessage(content)
    }
}
