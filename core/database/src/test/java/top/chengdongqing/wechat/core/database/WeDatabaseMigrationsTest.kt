package top.chengdongqing.wechat.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeDatabaseMigrationsTest {

    @Test
    fun `migration registry contains unique forward-only transitions`() {
        val migrations = WeDatabaseMigrations.all
        val transitionCount = migrations
            .groupBy { it.startVersion to it.endVersion }
            .size

        assertEquals(transitionCount, migrations.size)
        assertTrue(migrations.all { it.startVersion < it.endVersion })
    }
}
