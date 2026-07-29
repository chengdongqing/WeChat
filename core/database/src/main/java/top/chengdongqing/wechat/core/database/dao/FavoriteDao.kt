package top.chengdongqing.wechat.core.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.database.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Query(
        """SELECT * FROM favorites
           WHERE (:type = '' OR type = :type)
             AND (:query = '' OR title LIKE '%' || :query || '%'
             OR content LIKE '%' || :query || '%' OR sourceName LIKE '%' || :query || '%')
           ORDER BY updatedAt DESC"""
    )
    fun pagingSource(query: String, type: String): PagingSource<Int, FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<FavoriteEntity?>

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    suspend fun get(id: String): FavoriteEntity?

    @Query("SELECT * FROM favorites WHERE id IN (:ids) ORDER BY updatedAt DESC")
    suspend fun getByIds(ids: Set<String>): List<FavoriteEntity>

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE id IN (:ids)")
    suspend fun delete(ids: Set<String>)
}
