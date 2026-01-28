package top.chengdongqing.wechat.ui.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val DpSaver = Saver<Dp, Float>(
    save = { it.value },
    restore = { it.dp }
)