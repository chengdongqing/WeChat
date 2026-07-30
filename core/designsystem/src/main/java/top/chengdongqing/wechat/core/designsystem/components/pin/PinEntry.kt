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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun PinEntry(
    title: String,
    error: String?,
    onPinComplete: (String) -> Unit
) {
    var pin by remember(title, error) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(4) { index ->
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
        Spacer(Modifier.height(16.dp))
        Text(error.orEmpty(), color = WeTheme.colorScheme.danger, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        NumberPad(
            onDigit = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    if (pin.length == 4) {
                        val completed = pin
                        pin = ""
                        onPinComplete(completed)
                    }
                }
            },
            onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
        )
    }
}

@Composable
private fun NumberPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit -> NumberKey(digit) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(Modifier.size(72.dp))
            NumberKey("0") { onDigit("0") }
            NumberKey("⌫", onDelete)
        }
    }
}

@Composable
private fun NumberKey(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = if (text == "⌫") 21.sp else 29.sp,
            fontWeight = if (text == "⌫") FontWeight.Normal else FontWeight.Light,
            color = WeTheme.colorScheme.textPrimary
        )
    }
}
