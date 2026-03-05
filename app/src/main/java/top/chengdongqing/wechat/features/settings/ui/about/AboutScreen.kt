package top.chengdongqing.wechat.features.settings.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.util.getVersionName
import java.time.Year

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(
                containerColor = WeTheme.colorScheme.surface,
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AboutHeader()
                Spacer(modifier = Modifier.height(40.dp))
                AboutActionList()
            }

            AboutFooter()
        }
    }
}

@Composable
private fun AboutHeader() {
    val context = LocalContext.current
    val versionName = remember { context.getVersionName() }

    Spacer(modifier = Modifier.height(40.dp))
    Image(
        painter = painterResource(id = R.drawable.img_logo),
        contentDescription = "Logo",
        modifier = Modifier.size(60.dp)
    )
    Spacer(modifier = Modifier.height(22.dp))
    Text(
        text = stringResource(R.string.app_name),
        style = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = WeTheme.colorScheme.textPrimary
        )
    )
    Text(
        text = "Version $versionName",
        style = TextStyle(
            fontSize = 15.sp,
            color = WeTheme.colorScheme.textPrimary
        )
    )
}

@Composable
private fun AboutActionList() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        WeDivider()
        WeSettingItem(
            label = "功能介绍",
            onClick = {}
        )
        WeSettingItem(
            label = "投诉",
            onClick = {}
        )
        WeSettingItem(
            label = "版本更新",
            onClick = {},
            showDivider = false
        )
        WeDivider()
    }
}

@Composable
private fun BoxScope.AboutFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "项目说明：《掘金社区》",
                color = WeTheme.colorScheme.link,
                fontSize = 12.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "开源地址：《GitHub》《Gitee》",
                color = WeTheme.colorScheme.link,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "作者：James Lu",
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "邮箱：1912525497@qq.com",
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "提示：请在法律允许的范围内合规使用。",
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "腾讯公司 版权所有",
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "Copyright © 2011-${Year.now().value} Tencent. All Rights Reserved.",
            color = Color.LightGray,
            fontSize = 10.sp
        )
    }
}