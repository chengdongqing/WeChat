package top.chengdongqing.wechat.features.chat.ui.session.message.preview.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.DynamicBackground
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.MarqueeText
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.MusicControls
import top.chengdongqing.wechat.features.chat.ui.session.message.preview.music.components.VinylRecord

@Preview
@Composable
private fun Preview() {
    WeTheme {
        MusicPreviewScreen { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPreviewScreen(onBack: () -> Unit) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.36f) }
    val albumArt = R.drawable.img_album_art

    Box(modifier = Modifier.fillMaxSize()) {
        // 动态背景
        DynamicBackground(albumArt)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MarqueeText(
                                text = "Amazing Grace - The Snake Charmer",
                                style = TextStyle(color = Color.White, fontSize = 18.sp),
                                modifier = Modifier.width(200.dp)
                            )
                            Text(
                                "The Snake Charmer",
                                color = Color.White.copy(0.6f),
                                fontSize = 12.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back_outlined),
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(100.dp))

                // 唱片区
                VinylRecord(isPlaying = true, albumArt)

                // 控制区
                MusicControls(
                    progress = progress,
                    isPlaying = isPlaying,
                    onTogglePlay = { isPlaying = !isPlaying },
                    onProgressChange = { progress = it }
                )
            }
        }
    }
}