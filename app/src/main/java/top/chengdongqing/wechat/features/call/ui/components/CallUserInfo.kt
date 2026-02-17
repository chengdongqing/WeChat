package top.chengdongqing.wechat.features.call.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R

@Composable
fun CallUserInfo(
    userName: String,
    userAvatar: String?,
    statusText: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = userAvatar,
            contentDescription = null,
            error = painterResource(R.drawable.img_avatar_placeholder),
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            userName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        statusText?.let {
            Spacer(modifier = Modifier.height(40.dp))
            Text(it, fontSize = 14.sp, color = Color.White.copy(0.7f))
        }
    }
}