package top.chengdongqing.wechat.feature.settings.ui.display

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import top.chengdongqing.wechat.core.designsystem.R as DesignR
import top.chengdongqing.wechat.feature.settings.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

private enum class AppIconColor(
    val alias: String,
    @StringRes val labelRes: Int
) {
    Default("Default", R.string.app_icon_default),
    Black("Black", R.string.app_icon_black),
    White("White", R.string.app_icon_white),
    Blue("Blue", R.string.app_icon_blue),
    Purple("Purple", R.string.app_icon_purple),
    Pink("Pink", R.string.app_icon_pink),
    Orange("Orange", R.string.app_icon_orange),
    Red("Red", R.string.app_icon_red)
}

@OptIn(ExperimentalGridApi::class)
@Composable
fun AppIconSettingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppIconManager.current(context)) }

    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.display_app_icon),
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Grid(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp),
            config = {
                repeat(3) { column(1.fr) }
                gap(12.dp)
            }
        ) {
            AppIconColor.entries.forEach { option ->
                AppIconOption(
                    option = option,
                    selected = option == selected,
                    onClick = {
                        if (option != selected) {
                            AppIconManager.select(context, option)
                            selected = option
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppIconOption(
    option: AppIconColor,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WeTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AppIconPreview(
                option = option,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(WeTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIconPreview(
    option: AppIconColor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val image = remember(option) {
        val drawable = AppIconManager.icon(context, option) ?: return@remember null
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
        createBitmap(width, height).also { bitmap ->
            drawable.setBounds(0, 0, width, height)
            drawable.draw(Canvas(bitmap))
        }.asImageBitmap()
    }

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = stringResource(option.labelRes),
            modifier = modifier
        )
    }
}

private object AppIconManager {
    fun icon(context: Context, option: AppIconColor): Drawable? =
        context.packageManager.getActivityInfo(
            component(context, option),
            PackageManager.MATCH_DISABLED_COMPONENTS
        ).loadIcon(context.packageManager)

    fun current(context: Context): AppIconColor {
        val packageManager = context.packageManager
        return AppIconColor.entries.firstOrNull { option ->
            val state = packageManager.getComponentEnabledSetting(component(context, option))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (option == AppIconColor.Default &&
                            state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        } ?: AppIconColor.Default
    }

    fun select(context: Context, selected: AppIconColor) {
        val packageManager = context.packageManager
        packageManager.setComponentEnabledSetting(
            component(context, selected),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        AppIconColor.entries
            .filterNot { it == selected }
            .forEach { option ->
                packageManager.setComponentEnabledSetting(
                    component(context, option),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
    }

    fun component(context: Context, option: AppIconColor) = ComponentName(
        context.packageName,
        "${context.packageName}.launcher.${option.alias}"
    )
}
