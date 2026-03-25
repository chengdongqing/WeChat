package top.chengdongqing.wechat.core.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import top.chengdongqing.wechat.core.designsystem.R

enum class MusicTrack(
    val title: String,
    val artist: String,
    @get:DrawableRes val albumArtRes: Int,
    @get:RawRes val audioRes: Int
) {
    Perfect(
        title = "Perfect",
        artist = "Ed Sheeran",
        albumArtRes = R.drawable.img_album_art_perfect,
        audioRes = R.raw.music_perfect
    ),
    BravestMoment(
        title = "这是我一生中最勇敢的瞬间",
        artist = "棱镜乐队",
        albumArtRes = R.drawable.img_album_art_bravest_moment,
        audioRes = R.raw.music_bravest_moment
    )
}
