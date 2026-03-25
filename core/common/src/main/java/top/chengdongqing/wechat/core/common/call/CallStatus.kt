package top.chengdongqing.wechat.core.common.call

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R as DesignR

enum class CallStatus(
    @get:StringRes val descriptionRes: Int,
    @get:StringRes val descriptionForMeRes: Int
) {
    Cancelled(DesignR.string.call_status_cancelled_by_me, DesignR.string.call_status_cancelled),
    Declined(DesignR.string.call_status_declined, DesignR.string.call_status_declined_by_me),
    Finished(DesignR.string.call_status_finished, DesignR.string.call_status_finished),
    Missed(DesignR.string.call_status_missed, DesignR.string.call_status_missed_by_me),
    Failed(DesignR.string.call_status_failed, DesignR.string.call_status_failed);
}
