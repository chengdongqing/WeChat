package top.chengdongqing.wechat.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前会话管理
 */
@Singleton
class ActiveSessionManager @Inject constructor() {

    private val _activeSessionId = MutableStateFlow<String?>(null)
    private val _inChatList = MutableStateFlow(false)

    val inChat: Boolean
        get() = _activeSessionId.value != null || _inChatList.value

    val activeSessionId: String?
        get() = _activeSessionId.value

    fun enter(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun leave() {
        _activeSessionId.value = null
    }

    fun isActive(sessionId: String): Boolean {
        return _activeSessionId.value == sessionId
    }

    fun enterList() {
        _inChatList.value = true
    }

    fun leaveList() {
        _inChatList.value = false
    }
}