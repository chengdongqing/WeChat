package top.chengdongqing.wechat.ui.contacts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.Contact

@Composable
fun ContactItem(contact: Contact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White)
            .clickable {}
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.img_logo),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = contact.name, fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
fun ContactHeader(initial: String) {
    Text(
        text = initial,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color.Gray,
        fontSize = 14.sp
    )
}