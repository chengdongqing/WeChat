package top.chengdongqing.wechat.ui.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R

@Composable
fun ExpandablePanel(inputMode: ChatInputMode, defaultHeight: Dp = 300.dp) {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardHeight = with(density) { ime.getBottom(this).toDp() }
    val panelHeight = remember(keyboardHeight) {
        if (keyboardHeight > 0.dp) keyboardHeight else defaultHeight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(Color(0xFFF1F1F1))
    ) {
        when (inputMode) {
            ChatInputMode.EMOJI -> EmojiPanel()
            ChatInputMode.MORE -> MorePanel()
            else -> Unit
        }
    }
}

@Composable
private fun EmojiPanel() {
    LazyVerticalGrid(columns = GridCells.Fixed(7)) {
        items(40) { index ->
            Text("😀", modifier = Modifier.padding(12.dp), fontSize = 24.sp)
        }
    }
}

@Composable
private fun MorePanel() {
    val items = listOf("照片" to R.drawable.ic_album_outline, "拍摄" to R.drawable.ic_album_outline)
    LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(16.dp)) {
        items(items) { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(60.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(item.second), null, modifier = Modifier.size(30.dp))
                }
                Text(item.first, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
