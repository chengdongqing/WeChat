package top.chengdongqing.wechat.data.database.entity

data class EntityAudit(
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)