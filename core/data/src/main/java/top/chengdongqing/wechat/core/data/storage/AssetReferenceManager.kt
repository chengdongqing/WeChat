package top.chengdongqing.wechat.core.data.storage

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.chengdongqing.wechat.core.database.WeDatabase
import top.chengdongqing.wechat.core.database.dao.MediaAssetReferenceDao
import top.chengdongqing.wechat.core.database.dao.MediaFileDao
import top.chengdongqing.wechat.core.database.entity.MediaAssetReferenceEntity
import top.chengdongqing.wechat.core.database.entity.MediaFileEntity
import top.chengdongqing.wechat.core.file.PrivateFileManager
import javax.inject.Inject
import javax.inject.Singleton

enum class AssetOwnerType {
    Message,
    Contact,
    FriendRequest,
    ChatSession,
    Sticker,
    Music
}

data class AssetOwner(val type: AssetOwnerType, val id: String) {
    internal val databaseType: String get() = type.name.uppercase()
}

/**
 * The single write boundary for private-file ownership.
 *
 * References are idempotent owner records rather than a mutable counter. The same owner can be
 * attached repeatedly without inflating ownership, and replacing an owner's asset atomically
 * releases its previous file.
 */
@Singleton
class AssetReferenceManager @Inject constructor(
    private val database: WeDatabase,
    private val assetDao: MediaFileDao,
    private val referenceDao: MediaAssetReferenceDao,
    private val privateFileManager: PrivateFileManager
) {
    private val mutex = Mutex()

    suspend fun attach(localPath: String?, checksum: String, owner: AssetOwner) {
        if (localPath == null) return
        mutex.withLock {
            val orphaned = database.withWriteTransaction {
                val previousPaths = referenceDao.getPaths(owner.databaseType, owner.id)
                referenceDao.deleteOwner(owner.databaseType, owner.id)
                assetDao.insertIfAbsent(
                    MediaFileEntity(localPath = localPath, checksum = checksum)
                )
                assetDao.fillChecksum(localPath, checksum)
                referenceDao.insert(
                    MediaAssetReferenceEntity(
                        assetPath = localPath,
                        ownerType = owner.databaseType,
                        ownerId = owner.id
                    )
                )
                previousPaths.filter {
                    it != localPath && referenceDao.countForAsset(it) == 0
                }
            }
            deleteOrphans(orphaned)
        }
    }

    suspend fun detach(owner: AssetOwner) = mutex.withLock {
        val orphaned = database.withWriteTransaction {
            val paths = referenceDao.getPaths(owner.databaseType, owner.id)
            referenceDao.deleteOwner(owner.databaseType, owner.id)
            val orphanedPaths = paths.distinct()
                .filter { referenceDao.countForAsset(it) == 0 }
            orphanedPaths
        }
        deleteOrphans(orphaned)
    }

    suspend fun detachAll(type: AssetOwnerType, ownerIds: Collection<String>) {
        if (ownerIds.isEmpty()) return
        mutex.withLock {
            val databaseType = type.name.uppercase()
            val orphaned = database.withWriteTransaction {
                val paths = referenceDao.getPaths(databaseType, ownerIds)
                referenceDao.deleteOwners(databaseType, ownerIds)
                paths.distinct().filter { referenceDao.countForAsset(it) == 0 }
            }
            deleteOrphans(orphaned)
        }
    }

    /** Retries files left between reference removal and a previous interrupted disk deletion. */
    suspend fun cleanupOrphans() = mutex.withLock {
        val orphaned = database.withWriteTransaction {
            assetDao.getAllPaths().filter { referenceDao.countForAsset(it) == 0 }
        }
        deleteOrphans(orphaned)
    }

    private suspend fun deleteOrphans(paths: List<String>) {
        paths.distinct().forEach { path ->
            privateFileManager.deleteFile(path).getOrThrow()
            database.withWriteTransaction {
                if (referenceDao.countForAsset(path) == 0) assetDao.deleteByPath(path)
            }
        }
    }
}
