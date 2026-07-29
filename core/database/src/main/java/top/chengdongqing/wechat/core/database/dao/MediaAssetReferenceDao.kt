package top.chengdongqing.wechat.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import top.chengdongqing.wechat.core.database.entity.MediaAssetReferenceEntity

@Dao
interface MediaAssetReferenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reference: MediaAssetReferenceEntity)

    @Query("SELECT assetPath FROM media_asset_references WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun getPaths(ownerType: String, ownerId: String): List<String>

    @Query("SELECT assetPath FROM media_asset_references WHERE ownerType = :ownerType AND ownerId IN (:ownerIds)")
    suspend fun getPaths(ownerType: String, ownerIds: Collection<String>): List<String>

    @Query("DELETE FROM media_asset_references WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteOwner(ownerType: String, ownerId: String)

    @Query("DELETE FROM media_asset_references WHERE ownerType = :ownerType AND ownerId IN (:ownerIds)")
    suspend fun deleteOwners(ownerType: String, ownerIds: Collection<String>)

    @Query("SELECT COUNT(*) FROM media_asset_references WHERE assetPath = :assetPath")
    suspend fun countForAsset(assetPath: String): Int
}
