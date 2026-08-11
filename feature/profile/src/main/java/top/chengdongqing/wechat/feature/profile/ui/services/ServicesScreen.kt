package top.chengdongqing.wechat.feature.profile.ui.services

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.GreenPressed
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.profile.R
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onPaymentCode: () -> Unit,
    onWallet: () -> Unit,
    onBills: () -> Unit
) {
    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(R.string.services_title),
                onBack = onBack,
                actions = {
                    IconButton(
                        DesignR.drawable.ic_more_outlined,
                        description = stringResource(R.string.services_more)
                    )
                }
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 7.dp, top = 20.dp, end = 7.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenPressed)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroService(
                    title = stringResource(R.string.services_payments),
                    icon = R.drawable.ic_services_payment,
                    onClick = onPaymentCode
                )
                HeroService(
                    title = stringResource(R.string.services_wallet),
                    subtitle = "*****",
                    icon = R.drawable.ic_services_wallet,
                    onClick = onWallet
                )
            }

            Spacer(Modifier.height(7.dp))
            ServiceCard(stringResource(R.string.services_life)) {
                SmallService(
                    title = stringResource(R.string.services_utilities),
                    icon = R.drawable.ic_services_utilities,
                    onClick = onBills
                )
            }

            Spacer(Modifier.height(7.dp))
            ServiceCard(stringResource(R.string.services_transport)) {
                SmallService(
                    title = stringResource(R.string.services_tickets),
                    icon = R.drawable.ic_services_travel,
                    onClick = {}
                )
                SmallService(
                    title = stringResource(R.string.services_hotels),
                    icon = R.drawable.ic_services_hotel,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun HeroService(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(20.dp))
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp
        )
        Text(
            text = subtitle ?: "",
            color = Color.White.copy(alpha = .42f),
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ServiceCard(
    title: String,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp),
        shape = RoundedCornerShape(8.dp),
        color = WeTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(start = 10.dp, top = 14.dp, end = 10.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 5.dp),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
private fun SmallService(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .height(103.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = WeTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
