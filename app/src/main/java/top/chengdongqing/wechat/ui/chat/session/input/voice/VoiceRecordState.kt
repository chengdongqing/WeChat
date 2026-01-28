package top.chengdongqing.wechat.ui.chat.session.input.voice

enum class RecordStatus {
    IDLE,       // 初始状态
    RECORDING,  // 正在录音（松开即发送）
    CANCELING,  // 滑动到取消区域
    TRANSING    // 滑动到转文字区域
}