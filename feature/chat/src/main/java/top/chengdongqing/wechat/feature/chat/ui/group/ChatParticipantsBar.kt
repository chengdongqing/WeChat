package top.chengdongqing.wechat.feature.chat.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.components.button.DashedAddButton
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.R as DesignR

data class ChatParticipant(val id: String, val name: String, val avatarPath: String?)

@Composable
fun ChatParticipantsBar(
    participants: List<ChatParticipant>,
    onParticipantClick: (ChatParticipant) -> Unit = {},
    onAdd: () -> Unit,
    onManage: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        participants.take(5).forEach { participant ->
            Column(
                modifier = Modifier
                    .widthIn(max = 56.dp)
                    .onTap {
                        onParticipantClick(participant)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = participant.avatarPath,
                    error = painterResource(DesignR.drawable.img_avatar_placeholder),
                    contentDescription = participant.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    participant.name,
                    fontSize = 12.sp,
                    color = WeTheme.colorScheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DashedAddButton(
            modifier = Modifier.size(52.dp),
            cornerRadius = 6.dp,
            color = Color.Gray,
            onClick = onAdd
        )
        onManage?.let {
            DashedAddButton(
                modifier = Modifier.size(52.dp),
                cornerRadius = 6.dp,
                color = Color.Gray,
                icon = DesignR.drawable.ic_minus_outlined,
                onClick = it
            )
        }
    }
}
