package top.chengdongqing.wechat.data.sticker

data class Sticker(
    val stickerId: String,
    val localPath: String,
    val sortOrder: Int
)

val Stickers by lazy {
    listOf(
        "stickers/sticker_1.gif",
        "stickers/sticker_2.gif",
        "stickers/sticker_3.jpeg",
        "stickers/sticker_4.gif",
        "stickers/sticker_5.gif",
        "stickers/sticker_6.gif",
        "stickers/sticker_7.gif",
        "stickers/sticker_8.gif",
        "stickers/sticker_9.gif",
        "stickers/sticker_10.gif",
        "stickers/sticker_11.gif",
        "stickers/sticker_12.gif",
        "stickers/sticker_13.gif",
        "stickers/sticker_14.gif",
        "stickers/sticker_15.gif",
        "stickers/sticker_16.gif",
        "stickers/sticker_17.gif",
        "stickers/sticker_18.gif",
        "stickers/sticker_19.gif",
        "stickers/sticker_20.gif",
        "stickers/sticker_21.gif",
        "stickers/sticker_22.gif",
        "stickers/sticker_23.gif",
        "stickers/sticker_24.gif",
        "stickers/sticker_25.gif",
        "stickers/sticker_26.gif",
        "stickers/sticker_27.gif",
        "stickers/sticker_28.gif",
        "stickers/sticker_29.gif",
        "stickers/sticker_30.gif",
        "stickers/sticker_31.gif",
        "stickers/sticker_32.gif",
        "stickers/sticker_33.gif",
        "stickers/sticker_34.gif"
    ).mapIndexed { index, path ->
        Sticker(
            stickerId = path.substringAfter("/").substringBefore("."),
            localPath = path,
            sortOrder = index
        )
    }.sortedByDescending { it.sortOrder }
}