package top.chengdongqing.wechat.feature.moments.ui.cover

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.common.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.common.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.moments.ui.feed.MomentsViewModel

@Composable
fun ChangeMomentCoverScreen(
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onPhotographerWorks: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            viewModel.setCover(it)
            onChanged()
        }
    }
    val camera = rememberCameraLauncher { uri, _ ->
        viewModel.setCover(uri)
        onChanged()
    }
    Scaffold(
        topBar = { WeTopAppBar(title = "更换相册封面", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        Column(Modifier
            .padding(padding)
            .padding(top = 10.dp)) {
            CoverSourceRow("从手机相册选择", R.drawable.ic_album_filled) {
                album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            CoverSourceRow("拍一张", R.drawable.ic_camera_filled) {
                camera(VisualMediaType.Image)
            }
            CoverSourceRow("摄影师作品", R.drawable.ic_like_outlined, onPhotographerWorks)
        }
    }
}

@Composable
private fun CoverSourceRow(title: String, icon: Int, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(icon),
                null,
                Modifier.size(24.dp),
                tint = WeTheme.colorScheme.textPrimary
            )
            Text(title, Modifier
                .padding(start = 16.dp)
                .weight(1f), fontSize = 16.sp)
            Icon(
                painterResource(R.drawable.ic_right_outlined),
                null,
                Modifier.size(16.dp),
                tint = Color.Gray.copy(alpha = 0.6f)
            )
        }
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(start = 60.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PhotographerCoversScreen(
    onBack: () -> Unit,
    onChanged: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    var selecting by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { WeTopAppBar(title = "摄影师作品", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(PHOTOGRAPHER_COVERS) { cover ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !selecting) {
                            selecting = true
                            viewModel.setCoverFromUrl(cover.url) { success ->
                                selecting = false
                                if (success) onChanged()
                            }
                        }
                ) {
                    AsyncImage(
                        model = cover.url,
                        contentDescription = cover.author,
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        "摄影：${cover.author}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color(0x66000000))
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

private data class PhotographerCover(val author: String, val url: String)

private val PHOTOGRAPHER_COVERS = listOf(
    PhotographerCover("Jeremy Bishop", "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=1200"),
    PhotographerCover("Luca Bravo", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200"),
    PhotographerCover("Casey Horner", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200"),
    PhotographerCover("Simon Berger", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200"),
    PhotographerCover("Paul Gilmore", "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200"),
    PhotographerCover("Johannes Plenio", "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1200")
)
