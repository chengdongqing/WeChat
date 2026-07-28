package top.chengdongqing.wechat.core.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.data.R

/**
 * 可发送的音乐曲目。内置音乐使用资源 ID，本地/接收音乐使用文件路径。
 * [id] 保留旧枚举名称，因此旧数据库中的 Perfect/BravestMoment 仍可解析。
 */
@Serializable
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    @get:DrawableRes val albumArtRes: Int = 0,
    @get:RawRes val audioRes: Int = 0,
    val audioPath: String? = null,
    val coverPath: String? = null,
    val coverData: String? = null,
    val mimeType: String = "audio/*",
    val size: Long = 0
) {
    val name: String get() = id
    val isLocal: Boolean get() = !audioPath.isNullOrBlank()
    fun coverModel(): Any =
        coverPath?.takeIf { java.io.File(it).exists() }
            ?: coverData?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
            ?: albumArtRes

    companion object {
        val Perfect = MusicTrack(
            id = "Perfect",
            title = "Perfect",
            artist = "Ed Sheeran",
            albumArtRes = R.drawable.img_album_art_perfect,
            audioRes = R.raw.music_perfect
        )
        val BravestMoment = MusicTrack(
            id = "BravestMoment",
            title = "这是我一生中最勇敢的瞬间",
            artist = "棱镜乐队",
            albumArtRes = R.drawable.img_album_art_bravest_moment,
            audioRes = R.raw.music_bravest_moment
        )
        val entries = listOf(Perfect, BravestMoment)

        fun valueOf(name: String): MusicTrack =
            entries.firstOrNull { it.id == name } ?: throw IllegalArgumentException(name)
    }
}
