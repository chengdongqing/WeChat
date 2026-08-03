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

    private val migration5To6 = object : Migration(5, 6) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """CREATE TABLE IF NOT EXISTS `media_asset_references` (
                    `assetPath` TEXT NOT NULL, `ownerType` TEXT NOT NULL,
                    `ownerId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`assetPath`, `ownerType`, `ownerId`))""".trimIndent()
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_media_asset_references_ownerType_ownerId` " +
                        "ON `media_asset_references` (`ownerType`, `ownerId`)"
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_media_asset_references_assetPath` " +
                        "ON `media_asset_references` (`assetPath`)"
            )
            val now = System.currentTimeMillis()
            connection.execSQL(
                """CREATE TABLE `media_files_v6` (`localPath` TEXT NOT NULL,
                    `checksum` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`localPath`))"""
            )
            connection.execSQL(
                """INSERT OR IGNORE INTO media_files_v6(localPath, checksum, createdAt)
                    SELECT localPath, checksum, createdAt FROM media_files"""
            )
            connection.execSQL(
                """INSERT OR IGNORE INTO media_files_v6(localPath, checksum, createdAt)
                    SELECT localPath, '', $now FROM messages WHERE localPath IS NOT NULL"""
            )
            connection.execSQL(
                """INSERT OR IGNORE INTO media_files_v6(localPath, checksum, createdAt)
                    SELECT avatarPath, '', $now FROM contacts WHERE avatarPath IS NOT NULL"""
            )
            connection.execSQL(
                """INSERT OR IGNORE INTO media_files_v6(localPath, checksum, createdAt)
                    SELECT avatarPath, '', $now FROM friend_requests WHERE avatarPath IS NOT NULL"""
            )
            connection.execSQL("DROP TABLE media_files")
            connection.execSQL("ALTER TABLE media_files_v6 RENAME TO media_files")
            connection.execSQL(
                """INSERT OR IGNORE INTO media_asset_references(assetPath, ownerType, ownerId, createdAt)
                    SELECT localPath, 'MESSAGE', id, $now FROM messages WHERE localPath IS NOT NULL"""
            )
            connection.execSQL(
                """INSERT OR IGNORE INTO media_asset_references(assetPath, ownerType, ownerId, createdAt)
                    SELECT avatarPath, 'CONTACT', id, $now FROM contacts WHERE avatarPath IS NOT NULL"""
            )
            connection.execSQL(
                """INSERT OR IGNORE INTO media_asset_references(assetPath, ownerType, ownerId, createdAt)
                    SELECT avatarPath, 'FRIEND_REQUEST', id, $now FROM friend_requests WHERE avatarPath IS NOT NULL"""
            )
        }
    }

    private val migration6To7 = object : Migration(6, 7) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """CREATE TABLE IF NOT EXISTS `favorites` (
                    `id` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL, `mediaPaths` TEXT NOT NULL,
                    `sourceMessageIds` TEXT NOT NULL, `sourceName` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`))""".trimIndent()
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_favorites_createdAt` ON `favorites` (`createdAt`)"
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_favorites_type` ON `favorites` (`type`)"
            )
        }
    }

    private val migration7To8 = object : Migration(7, 8) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `messages` ADD COLUMN `attemptCount` INTEGER NOT NULL DEFAULT 0"
            )
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `lastAttemptAt` INTEGER")
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `nextRetryAt` INTEGER")
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `ackDeadlineAt` INTEGER")
            connection.execSQL("ALTER TABLE `messages` ADD COLUMN `lastTransportType` TEXT")
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_messages_sendStatus_nextRetryAt` " +
                        "ON `messages` (`sendStatus`, `nextRetryAt`)"
            )
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `chat_sessions` ADD COLUMN `isTemporary` INTEGER NOT NULL DEFAULT 0"
            )
            connection.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `expiresAt` INTEGER")
            connection.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `temporaryPeerPublicKey` TEXT")
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_chat_sessions_isTemporary_expiresAt` " +
                        "ON `chat_sessions` (`isTemporary`, `expiresAt`)"
            )
        }
    }

    private val migration9To10 = object : Migration(9, 10) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE `chat_sessions` ADD COLUMN `isBottomed` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val all: Array<Migration> =
        arrayOf(
            migration1To2, migration2To3, migration3To4, migration4To5,
            migration5To6, migration6To7, migration7To8, migration8To9, migration9To10
        )
}
