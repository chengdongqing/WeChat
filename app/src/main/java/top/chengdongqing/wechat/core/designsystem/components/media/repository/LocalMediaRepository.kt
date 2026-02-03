package top.chengdongqing.wechat.core.designsystem.components.media.repository

import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaType

interface LocalMediaRepository {
    suspend fun loadMediaList(types: Array<MediaType>): List<MediaItem>
}