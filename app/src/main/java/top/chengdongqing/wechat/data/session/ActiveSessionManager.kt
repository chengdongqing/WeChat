package top.chengdongqing.wechat.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前会话管理
 * 方便处理已进入的会话不发送通知，不计入未读
 */
@Singleton
class ActiveSessionManager @Inject constructor() {

    private val _activeSessionId = MutableStateFlow<String?>(null)

    fun enter(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun leave() {
        _activeSessionId.value = null
    }

    fun isActive(sessionId: String): Boolean {
        return _activeSessionId.value == sessionId
    }
}