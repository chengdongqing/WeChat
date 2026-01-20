package top.chengdongqing.wechat.ui.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphabetIndexer(modifier: Modifier, initials: List<String>) {
    Column(
        modifier = modifier.padding(end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        initials.forEach { char ->
            Text(
                text = char,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}