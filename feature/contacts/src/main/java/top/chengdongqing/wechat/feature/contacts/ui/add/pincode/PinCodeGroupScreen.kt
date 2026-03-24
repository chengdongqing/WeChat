package top.chengdongqing.wechat.feature.contacts.ui.add.pincode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.GreenPrimary
import top.chengdongqing.wechat.core.designsystem.util.NeonGreenIndication
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect

@Composable
fun PinCodeGroupScreen(onBack: () -> Unit) {
    var inputCode by remember { mutableStateOf("") }

    StatusBarAppearanceEffect(false)
    Scaffold(
        containerColor = Color(0xFF171F1E),
        topBar = {
            WeTopBar(
                title = stringResource(R.string.add_contact_option_face_to_face_title),
                containerColor = Color.Unspecified,
                contentColor = Color.White,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_contact_option_face_to_face_desc_full),
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            DigitCodeInput(inputCode)
            Spacer(modifier = Modifier.weight(1f))
            CustomNumberKeyboard(
                onNumberClick = { if (inputCode.length < 4) inputCode += it },
                onDelete = { if (inputCode.isNotEmpty()) inputCode = inputCode.dropLast(1) }
            )
        }
    }
}

@Composable
private fun DigitCodeInput(code: String) {
    Row(
        modifier = Modifier.padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val char = code.getOrNull(index)

            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (char != null) {
                    // 已输入：显示数字
                    Text(
                        text = char.toString(),
                        style = TextStyle(
                            color = GreenPrimary,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Thin,
                            shadow = Shadow(color = GreenPrimary, blurRadius = 12f)
                        )
                    )
                } else {
                    // 未输入：黑色原点
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF07C160).copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomNumberKeyboard(
    onNumberClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")

    Column(modifier = Modifier.fillMaxWidth()) {
        keys.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = NeonGreenIndication
                            ) {
                                when (key) {
                                    "DEL" -> onDelete()
                                    "" -> {}
                                    else -> onNumberClick(key)
                                }
                            }
                            .border(0.2.dp, Color(0xFF2B2929)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "DEL") {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        } else {
                            Text(
                                text = key,
                                color = Color.Gray,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}