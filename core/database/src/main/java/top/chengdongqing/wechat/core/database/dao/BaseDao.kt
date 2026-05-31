package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Update

interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)

    @Update
    suspend fun update(entity: T)
}