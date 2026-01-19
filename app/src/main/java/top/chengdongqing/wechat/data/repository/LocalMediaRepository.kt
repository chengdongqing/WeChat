package top.chengdongqing.wechat.data.repository

import top.chengdongqing.wechat.data.model.MediaItem
import top.chengdongqing.wechat.data.model.MediaType

interface LocalMediaRepository {
    suspend fun loadMediaList(types: Array<MediaType>): List<MediaItem>
}