package top.chengdongqing.wechat.core.database.entity

data class EntityAudit(
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)