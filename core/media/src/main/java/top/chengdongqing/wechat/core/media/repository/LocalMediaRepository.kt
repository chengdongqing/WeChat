package top.chengdongqing.wechat.core.media.repository

import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.media.model.MediaType

interface LocalMediaRepository {
    suspend fun loadMediaList(types: Array<MediaType>): List<MediaItem>
}
