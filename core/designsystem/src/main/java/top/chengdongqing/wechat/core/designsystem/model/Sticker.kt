package top.chengdongqing.wechat.core.designsystem.model

data class Sticker(
    val localPath: String,
    val orderNo: Int
)

object Stickers {
    private val rawPaths = listOf(
        "stickers/sticker_1.webp",
        "stickers/sticker_3.webp",
        "stickers/sticker_4.webp",
        "stickers/sticker_5.webp",
        "stickers/sticker_6.webp",
        "stickers/sticker_7.webp",
        "stickers/sticker_8.webp",
        "stickers/sticker_9.webp",
        "stickers/sticker_10.webp",
        "stickers/sticker_11.webp",
        "stickers/sticker_12.webp",
        "stickers/sticker_13.webp",
        "stickers/sticker_14.webp",
        "stickers/sticker_15.webp",
        "stickers/sticker_16.webp",
        "stickers/sticker_18.webp",
        "stickers/sticker_19.webp",
        "stickers/sticker_20.webp",
        "stickers/sticker_21.webp",
        "stickers/sticker_22.webp",
        "stickers/sticker_23.webp",
        "stickers/sticker_24.webp",
        "stickers/sticker_25.webp",
        "stickers/sticker_26.webp",
        "stickers/sticker_27.webp",
        "stickers/sticker_28.webp",
        "stickers/sticker_29.webp",
        "stickers/sticker_31.webp",
        "stickers/sticker_32.webp",
        "stickers/sticker_33.webp",
        "stickers/sticker_34.webp"
    )

    val all: List<Sticker> = buildStickerList()

    private fun buildStickerList() = rawPaths
        .mapIndexed { index, path ->
            Sticker(
                localPath = path,
                orderNo = index
            )
        }
        .sortedByDescending { it.orderNo }
}