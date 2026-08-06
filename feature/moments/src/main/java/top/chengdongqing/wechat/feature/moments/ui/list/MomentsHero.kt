package top.chengdongqing.wechat.feature.moments.ui.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.media.isLandscape
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.DarkElevated
import top.chengdongqing.wechat.core.model.UserProfile

@Composable
internal fun MomentsHero(
    cover: String?,
    profile: UserProfile,
    expanded: Boolean,
    onCoverClick: () -> Unit,
    onChangeCover: () -> Unit,
    onProfileClick: () -> Unit
) {
    val containerHeight = LocalWindowInfo.current.containerDpSize.height
    val animatedHeroHeight by animateDpAsState(
        targetValue = if (expanded) containerHeight * 0.8f else 290.dp,
        label = "MomentsCoverHeight"
    )
    val isLandscape by produceState(false, cover) {
        cover?.let {
            value = isLandscape(it)
        }
    }
    BackHandler(expanded, onCoverClick)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onTap(onClick = onCoverClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeroHeight)
                .background(DarkElevated)
        ) {
            if (cover != null) {
                BlurBackground(cover)
                AsyncImage(
                    model = cover,
                    contentDescription = "朋友圈封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (expanded && isLandscape) ContentScale.Fit else ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp)
        ) {
            if (expanded) {
                CoverButton(onChangeCover)
            } else {
                HeroProfile(profile, onProfileClick)
            }
        }
    }
}

@Composable
private fun CoverButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(bottom = 14.dp)
            .onTap(
                role = Role.Button,
                onClickLabel = "换封面",
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_album_filled),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "换封面",
            color = Color.White,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun BlurBackground(cover: Any) {
    AsyncImage(
        model = cover,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .blur(34.dp),
        contentScale = ContentScale.Crop,
        alpha = 0.72f
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.42f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.46f)
                    )
                )
            )
    )
}

@Composable
private fun HeroProfile(
    profile: UserProfile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .offset(y = 20.dp)
            .onTap(
                role = Role.Button,
                onClickLabel = "我的个人资料",
                onClick = onClick
            )
    ) {
        Text(
            text = profile.nickname,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.width(12.dp))
        AsyncImage(
            model = profile.avatarPath ?: R.drawable.img_avatar_placeholder,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
