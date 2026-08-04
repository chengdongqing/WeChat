package top.chengdongqing.wechat.core.designsystem.components.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun PinEntry(
    title: String,
    error: String?,
    modifier: Modifier = Modifier,
    pinLength: Int = 4,
    onPinComplete: (String) -> Unit
) {
    var pin by remember(title, error) { mutableStateOf("") }

    LaunchedEffect(error) {
        if (!error.isNullOrEmpty()) {
            pin = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Spacer(Modifier.height(48.dp))

        // 指示灯圆点
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(pinLength) { index ->
                Box(
                    Modifier
                        .size(14.dp)
                        .background(
                            if (index < pin.length) {
                                WeTheme.colorScheme.textPrimary
                            } else {
                                WeTheme.colorScheme.textSecondary.copy(alpha = .25f)
                            },
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        Text(
            text = error.orEmpty(),
            color = WeTheme.colorScheme.danger,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(48.dp))

        NumberPad(
            onDigit = { digit ->
                if (pin.length < pinLength) {
                    val newPin = pin + digit
                    pin = newPin
                    if (newPin.length == pinLength) {
                        onPinComplete(newPin)
                    }
                }
            },
            onDelete = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }
            }
        )
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
    val keys = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "backspace")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "backspace" -> DeleteKey(onDelete)
                        "" -> KeyPlaceholder()
                        else -> NumberKey(key) { onDigit(key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    digit: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(WeTheme.colorScheme.surface)
            .clickable(
                role = Role.Button,
                onClickLabel = digit,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
    }
}

@Composable
private fun DeleteKey(onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = "退格",
                onClick = onDelete
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = null,
            tint = WeTheme.colorScheme.textPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun KeyPlaceholder() {
    Box(modifier = Modifier.size(72.dp))
}
