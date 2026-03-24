package top.chengdongqing.wechat.core.designsystem.components.actionsheet

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

data class ActionSheetItem(
    @get:StringRes val labelRes: Int,
    @get:StringRes val descriptionRes: Int? = null,
    val color: Color? = null,
    val disabled: Boolean = false,
    val value: Any? = null,
    val icon: (@Composable () -> Unit)? = null
)

/**
 * 弹出式菜单
 *
 * @param visible 是否显示
 * @param title 标题
 * @param options 菜单列表
 * @param onCancel 取消事件
 * @param onTap 菜单选中事件
 */
@Composable
fun WeActionSheet(
    visible: Boolean,
    title: String? = null,
    options: List<ActionSheetItem>,
    onCancel: () -> Unit,
    onTap: (index: Int) -> Unit
) {
    WePopup(
        visible = visible,
        padding = PaddingValues(0.dp),
        draggable = false,
        onClose = onCancel
    ) {
        Column {
            title?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(56.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = it,
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            options.forEachIndexed { index, item ->
                if (index > 0 || title != null) {
                    WeDivider()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(56.dp)
                        .alpha(if (item.disabled) 0.4f else 1f)
                        .then(
                            if (!item.disabled) {
                                Modifier.clickable {
                                    onCancel()
                                    onTap(index)
                                }
                            } else Modifier)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.icon?.let { icon ->
                            icon()
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = stringResource(item.labelRes),
                            color = item.color ?: WeTheme.colorScheme.textPrimary,
                            fontSize = 16.sp
                        )
                    }
                    item.descriptionRes?.let {
                        Text(
                            text = stringResource(it),
                            color = WeTheme.colorScheme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
                    .background(WeTheme.colorScheme.surfaceVariant)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        onCancel()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Stable
interface ActionSheetState {
    /**
     * 是否显示
     */
    val visible: Boolean

    /**
     * 显示菜单
     */
    fun show(
        options: List<ActionSheetItem>,
        @StringRes title: Int? = null,
        onChange: (index: Int) -> Unit
    )

    /**
     * 隐藏菜单
     */
    fun hide()
}

@Composable
fun rememberActionSheetState(): ActionSheetState {
    val state = remember { ActionSheetStateImpl() }

    state.props?.let { props ->
        WeActionSheet(
            visible = state.visible,
            title = props.title?.let { stringResource(it) },
            options = props.options,
            onCancel = { state.hide() },
            onTap = props.onChange
        )
    }

    return state
}

private class ActionSheetStateImpl : ActionSheetState {
    override var visible by mutableStateOf(false)
    var props by mutableStateOf<ActionSheetProps?>(null)
        private set

    override fun show(
        options: List<ActionSheetItem>,
        @StringRes title: Int?,
        onChange: (index: Int) -> Unit
    ) {
        props = ActionSheetProps(options, title, onChange)
        visible = true
    }

    override fun hide() {
        visible = false
    }
}

@Immutable
private data class ActionSheetProps(
    val options: List<ActionSheetItem>,
    @get:StringRes val title: Int? = null,
    val onChange: (index: Int) -> Unit
)