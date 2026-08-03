package top.chengdongqing.wechat.feature.chat.ui.group

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.file.PrivateFileManager
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ChatSessionDao
import top.chengdongqing.wechat.core.database.dao.GroupDao
import top.chengdongqing.wechat.core.database.entity.GroupMemberRole
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.network.messaging.GroupChatCoordinator
import top.chengdongqing.wechat.core.util.showToast

data class GroupMemberUiState(
    val id: String,
    val name: String,
    val avatarPath: String?
)

data class GroupInfoUiState(
    val groupName: String = "",
    val announcement: String = "",
    val remark: String = "",
    val members: List<GroupMemberUiState> = emptyList(),
    val memberCount: Int = 0,
    val myNickname: String = "",
    val canManageMembers: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isBottomed: Boolean = false,
    val isFolded: Boolean = false,
    val saveToContacts: Boolean = true,
    val showMemberNicknames: Boolean = true,
    val backgroundPath: String? = null
)

@HiltViewModel(assistedFactory = GroupInfoViewModel.Factory::class)
class GroupInfoViewModel @AssistedInject constructor(
    @Assisted val groupId: String,
    private val groupDao: GroupDao,
    private val chatSessionDao: ChatSessionDao,
    private val chatSessionRepository: ChatSessionRepository,
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager,
    private val groupChatCoordinator: GroupChatCoordinator,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(groupId: String): GroupInfoViewModel
    }

    val uiState = combine(
        groupDao.observeById(groupId),
        groupDao.observeMembers(groupId),
        chatSessionRepository.observeSession(groupId),
        profileRepository.observeProfile(),
    ) { values ->
        val group = values[0] as? top.chengdongqing.wechat.core.database.entity.GroupEntity
        @Suppress("UNCHECKED_CAST")
        val members = values[1] as List<top.chengdongqing.wechat.core.database.entity.GroupMemberEntity>
        val session = values[2] as? top.chengdongqing.wechat.core.model.ChatSession
        val profile = values[3] as? top.chengdongqing.wechat.core.model.UserProfile
        GroupInfoUiState(
            groupName = group?.name.orEmpty(),
            announcement = group?.announcement.orEmpty(),
            remark = group?.remark.orEmpty(),
            members = members.map { GroupMemberUiState(it.userId, it.nickname, it.avatarPath) },
            memberCount = members.size,
            myNickname = members.firstOrNull { it.userId == profile?.id }?.nickname
                ?: profile?.nickname.orEmpty(),
            canManageMembers = members.any {
                it.userId == profile?.id &&
                    (it.role == GroupMemberRole.Owner || it.role == GroupMemberRole.Admin)
            },
            isMuted = session?.isMuted == true,
            isPinned = session?.isPinned == true,
            isBottomed = session?.isBottomed == true,
            isFolded = group?.isFolded == true,
            saveToContacts = group?.savedToContacts != false,
            showMemberNicknames = group?.showMemberNicknames != false,
            backgroundPath = session?.backgroundPath
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupInfoUiState())

    fun setFolded(value: Boolean) = updateGroup { groupDao.updateFolded(groupId, value) }
    fun setSaveToContacts(value: Boolean) = updateGroup { groupDao.updateSavedToContacts(groupId, value) }
    fun setShowMemberNicknames(value: Boolean) =
        updateGroup { groupDao.updateShowMemberNicknames(groupId, value) }
    fun updateName(value: String) = updateGroup(sync = true) {
        val name = value.trim()
        if (name.isNotEmpty()) {
            groupDao.updateName(groupId, name)
            chatSessionDao.update(groupId) { it.copy(contactName = name) }
        }
    }
    fun updateAnnouncement(value: String) = updateGroup(sync = true) {
        groupDao.updateAnnouncement(groupId, value.trim().takeIf(String::isNotEmpty))
    }
    fun updateRemark(value: String) = updateGroup {
        groupDao.updateRemark(groupId, value.trim().takeIf(String::isNotEmpty))
    }
    fun updateMyNickname(value: String) = updateGroup(sync = true) {
        val profile = profileRepository.requireProfile()
        val nickname = value.trim()
        if (nickname.isNotEmpty()) groupDao.updateMemberNickname(groupId, profile.id, nickname)
    }

    fun addMembers(contacts: Array<ContactResult>) = updateGroup(sync = true) {
        val me = profileRepository.requireUserId()
        val existingIds = uiState.value.members.mapTo(hashSetOf()) { it.id }
        groupDao.upsertMembers(contacts.filterNot { it.id == me || it.id in existingIds }.map {
            top.chengdongqing.wechat.core.database.entity.GroupMemberEntity(
                groupId = groupId,
                userId = it.id,
                nickname = it.nickname,
                avatarPath = it.avatarPath
            )
        })
    }

    fun removeMember(userId: String) = updateGroup(sync = true) {
        val profile = profileRepository.requireProfile()
        val group = groupDao.getById(groupId) ?: return@updateGroup
        if (userId != profile.id && userId != group.ownerId) {
            groupDao.removeMember(groupId, userId)
        }
    }

    fun exitGroup() = updateGroup {
        chatSessionRepository.deleteSession(groupId)
        groupDao.deleteGroup(groupId)
    }

    private fun updateGroup(sync: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                block()
                if (sync) groupChatCoordinator.syncGroup(groupId).getOrThrow()
            }.onFailure {
                Log.e("GroupInfoVM", "更新群聊失败", it)
                context.showToast("操作失败")
            }
        }
    }

    fun setMuted(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { chatSessionRepository.toggleMute(groupId, value) }
    }

    fun setPinned(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { chatSessionRepository.togglePin(groupId, value) }
    }

    fun setBottomed(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { chatSessionRepository.toggleBottom(groupId, value) }
    }

    fun updateBackground(uri: Uri?) {
        viewModelScope.launch {
            try {
                val oldPath = uiState.value.backgroundPath
                val newPath = uri?.let {
                    privateFileManager.saveMedia(MessageType.Image, it).getOrThrow()
                }
                chatSessionRepository.updateBackground(groupId, newPath)
                oldPath?.let { privateFileManager.deleteFile(it) }
                context.showToast(if (uri == null) "背景清除成功" else "背景设置成功")
            } catch (e: Exception) {
                Log.e("GroupInfoVM", "更新群聊背景失败", e)
                context.showToast("背景设置失败")
            }
        }
    }

    fun clearMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.deleteSession(groupId, false)
        }
    }
}
