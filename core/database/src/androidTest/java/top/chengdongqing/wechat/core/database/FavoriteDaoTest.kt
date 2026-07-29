package top.chengdongqing.wechat.core.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import top.chengdongqing.wechat.core.database.entity.FavoriteEntity

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {
    private lateinit var database: WeDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WeDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagingQueryCombinesKeywordAndTypeAndOrdersByUpdateTime() = runBlocking {
        val dao = database.favoriteDao()
        dao.upsertAll(
            listOf(
                favorite("note", "RICH_TEXT", "旅行清单", "护照", 100),
                favorite("voice", "VOICE", "旅行录音", "北京", 300),
                favorite("media", "MEDIA", "旅行照片", "北京", 200)
            )
        )

        val allResults = load(dao.pagingSource("旅行", ""))
        assertEquals(listOf("voice", "media", "note"), allResults.map { it.id })

        val mediaResults = load(dao.pagingSource("北京", "MEDIA"))
        assertEquals(listOf("media"), mediaResults.map { it.id })
    }

    @Test
    fun deleteRemovesOnlySelectedFavorites() = runBlocking {
        val dao = database.favoriteDao()
        dao.upsertAll(
            listOf(
                favorite("one", "RICH_TEXT", "一", "", 1),
                favorite("two", "VOICE", "二", "", 2)
            )
        )

        dao.delete(setOf("one"))

        assertTrue(dao.get("one") == null)
        assertEquals("two", dao.get("two")?.id)
    }

    private suspend fun load(source: PagingSource<Int, FavoriteEntity>): List<FavoriteEntity> {
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    private fun favorite(
        id: String,
        type: String,
        title: String,
        content: String,
        updatedAt: Long
    ) = FavoriteEntity(
        id = id,
        type = type,
        title = title,
        content = content,
        createdAt = updatedAt,
        updatedAt = updatedAt
    )
}
