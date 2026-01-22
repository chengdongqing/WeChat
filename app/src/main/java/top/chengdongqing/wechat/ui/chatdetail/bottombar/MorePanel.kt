package top.chengdongqing.wechat.ui.chatdetail.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R

@Composable
fun MorePanel() {
    val items = remember {
        listOf("照片" to R.drawable.ic_album_outlined, "拍摄" to R.drawable.ic_album_outlined)
    }

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
