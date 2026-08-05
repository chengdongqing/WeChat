package top.chengdongqing.wechat.feature.moments.ui.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.moments.ui.list.MomentsViewModel

@Composable
fun PhotographerWorksScreen(
    onBack: () -> Unit,
    onChanged: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    var selecting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { WeTopAppBar(title = "摄影师作品", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = innerPadding.plus(PaddingValues(top = 20.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            overscrollEffect = rememberBouncedOverscrollEffect(),
            modifier = Modifier.fillMaxSize()
        ) {
            items(PhotographerWorks) { cover ->
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
                        "摄影师：${cover.author}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color(0x66000000))
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

private data class PhotographerWork(
    val author: String,
    val url: String
)

private val PhotographerWorks = listOf(
    PhotographerWork(
        "Jeremy Bishop",
        "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=1200"
    ),
    PhotographerWork(
        "Luca Bravo",
        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1200"
    ),
    PhotographerWork(
        "Casey Horner",
        "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1200"
    ),
    PhotographerWork(
        "Simon Berger",
        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200"
    ),
    PhotographerWork(
        "Paul Gilmore",
        "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200"
    ),
    PhotographerWork(
        "Johannes Plenio",
        "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1200"
    )
)
