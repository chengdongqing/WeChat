package top.chengdongqing.wechat.features.call.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

/**
 * 通话状态（结果）
 */
enum class CallStatus(
    @get:StringRes val descriptionRes: Int,
    @get:StringRes val descriptionForMeRes: Int
) {
    Cancelled(R.string.call_status_cancelled, R.string.call_status_cancelled_by_me),
    Declined(R.string.call_status_declined, R.string.call_status_declined_by_me),
    Finished(R.string.call_status_finished, R.string.call_status_finished),
    Missed(R.string.call_status_missed, R.string.call_status_missed_by_me),
    Failed(R.string.call_status_failed, R.string.call_status_failed);
}