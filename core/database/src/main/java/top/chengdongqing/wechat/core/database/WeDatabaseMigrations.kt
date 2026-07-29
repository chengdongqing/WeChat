package top.chengdongqing.wechat.core.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The only registry for production database migrations.
 *
 * Add each schema transition here when [WeDatabase] is versioned up. Destructive
 * migration is intentionally not an upgrade strategy: user messages, contacts and
 * settings must survive an app update.
 */
object WeDatabaseMigrations {
    private val migration1To2 = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """CREATE TABLE IF NOT EXISTS `groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL,
                    `ownerId` TEXT NOT NULL, `avatarPath` TEXT, `announcement` TEXT,
                    `memberVersion` INTEGER NOT NULL, `meshEnabled` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`))""".trimIndent()
            )
            connection.execSQL(
                """CREATE TABLE IF NOT EXISTS `group_members` (`groupId` TEXT NOT NULL,
                    `userId` TEXT NOT NULL, `nickname` TEXT NOT NULL, `avatarPath` TEXT,
                    `role` TEXT NOT NULL, `joinedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`groupId`, `userId`),
                    FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
            )
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_group_members_groupId` ON `group_members` (`groupId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_group_members_userId` ON `group_members` (`userId`)")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """CREATE TABLE IF NOT EXISTS `contact_tags` (`id` TEXT NOT NULL,
                    `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""".trimIndent()
            )
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_contact_tags_name` ON `contact_tags` (`name`)"
            )
            connection.execSQL(
                """CREATE TABLE IF NOT EXISTS `contact_tag_members` (`tagId` TEXT NOT NULL,
                    `contactId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`tagId`, `contactId`),
                    FOREIGN KEY(`tagId`) REFERENCES `contact_tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`contactId`) REFERENCES `contacts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_contact_tag_members_tagId` ON `contact_tag_members` (`tagId`)"
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_contact_tag_members_contactId` ON `contact_tag_members` (`contactId`)"
            )
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `groups` ADD COLUMN `remark` TEXT")
            connection.execSQL("ALTER TABLE `groups` ADD COLUMN `savedToContacts` INTEGER NOT NULL DEFAULT 1")
            connection.execSQL("ALTER TABLE `groups` ADD COLUMN `showMemberNicknames` INTEGER NOT NULL DEFAULT 1")
            connection.execSQL("ALTER TABLE `groups` ADD COLUMN `isFolded` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `quoteMessageId` TEXT")
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `quoteSenderId` TEXT")
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `quoteMessageType` TEXT")
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `quotePreview` TEXT")
        }
    }

    val all: Array<Migration> =
        arrayOf(migration1To2, migration2To3, migration3To4, migration4To5)
}
