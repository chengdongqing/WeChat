package top.chengdongqing.wechat.core.common.qrcode.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.common.media.model.VisualMediaType
import top.chengdongqing.wechat.core.common.media.picker.rememberMediaPickerLauncher
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
internal fun BoxScope.ScannerTools(state: ScannerState) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val pickMedia = rememberMediaPickerLauncher { medias, _, _ ->
        state.scanPhoto(medias.first().uri) {
            context.showToast(resources.getString(DesignR.string.scan_recognize_failed))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ToolItem(
            label = stringResource(DesignR.string.scan_tool_flash),
            icon = if (state.isFlashOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
            iconColor = if (state.isFlashOn) WeTheme.colorScheme.primary else Color.White
        ) {
            state.toggleFlashState()
        }
        ToolItem(
            label = stringResource(DesignR.string.scan_tool_album),
            icon = Icons.Filled.Image
        ) {
            pickMedia(VisualMediaType.Image, 1)
            if (state.isFlashOn) {
                state.toggleFlashState()
            }
        }
    }
}

@Composable
private fun ToolItem(
    label: String,
    icon: ImageVector,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(0.2f))
                .clickable { onClick() }
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = Color.White, fontSize = 13.sp)
    }
}
