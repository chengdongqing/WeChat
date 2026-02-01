package top.chengdongqing.wechat.data.model

enum class MapType(val appName: String) {
    AMap("高德"),
    Baidu("百度"),
    Tencent("腾讯"),
    Google("谷歌");

    companion object {
        fun ofIndex(index: Int): MapType? {
            return entries.getOrNull(index)
        }
    }
}