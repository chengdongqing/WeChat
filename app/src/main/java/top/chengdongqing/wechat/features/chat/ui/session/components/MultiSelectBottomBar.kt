package top.chengdongqing.wechat.features.chat.ui.session.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.features.chat.ui.session.message.MultiMessageAction

@Composable
fun MultiSelectBottomBar(
    enabled: Boolean,
    onActionClick: (MultiMessageAction) -> Unit,
    onExitSelectMode: () -> Unit
) {
    BackHandler(true) {
        onExitSelectMode()
    }

    Row(
        modifier = Modifier
            .height(76.dp)
            .background(White)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        MultiMessageAction.entries.forEach { action ->
            ActionButton(action, enabled) {
                onActionClick(action)
            }
        }
    }
}

/**
 * 单个操作按钮
 */
@Composable
private fun RowScope.ActionButton(
    action: MultiMessageAction,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = stringResource(action.labelRes),
            tint = Color.Black.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}