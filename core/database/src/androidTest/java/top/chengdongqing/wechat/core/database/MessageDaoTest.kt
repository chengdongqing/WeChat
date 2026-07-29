package top.chengdongqing.wechat.core.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import top.chengdongqing.wechat.core.database.entity.MessageEntity
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.model.SendStatus

@RunWith(AndroidJUnit4::class)
class MessageDaoTest {
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
    fun pendingOutgoingReturnsOnlyRetryCandidatesForPeerInTimestampOrder() = runBlocking {
        val dao = database.messageDao()
        dao.insert(message("delivered", "peer-a", SendStatus.Delivered, 1))
        dao.insert(message("sent", "peer-a", SendStatus.Sent, 3))
        dao.insert(message("failed", "peer-a", SendStatus.Failed, 2))
        dao.insert(message("other-peer", "peer-b", SendStatus.Failed, 0))

        val pending = dao.getPendingOutgoing("peer-a")

        assertEquals(listOf("failed", "sent"), pending.map { it.id })
    }

    @Test
    fun dueOutgoingReturnsExpiredAckAndScheduledFailureOnly() = runBlocking {
        val dao = database.messageDao()
        dao.insert(
            message("ack-expired", "peer", SendStatus.Sent, 1)
                .copy(attemptCount = 1, ackDeadlineAt = 100)
        )
        dao.insert(
            message("retry-due", "peer", SendStatus.Failed, 2)
                .copy(attemptCount = 2, nextRetryAt = 100)
        )
        dao.insert(
            message("future", "peer", SendStatus.Sent, 3)
                .copy(attemptCount = 1, ackDeadlineAt = 1_000)
        )

        val due = dao.getDueOutgoing(now = 200, maxAttempts = 6)

        assertEquals(listOf("ack-expired", "retry-due"), due.map { it.id })
    }

    private fun message(
        id: String,
        receiverId: String,
        status: SendStatus,
        timestamp: Long
    ) = MessageEntity(
        id = id,
        sessionId = receiverId,
        senderId = "me",
        receiverId = receiverId,
        contentType = MessageType.Text,
        content = id,
        timestamp = timestamp,
        sendStatus = status,
        isFromMe = true,
        failReason = if (status == SendStatus.Failed) SendError.ConnectionFailed else null
    )
}
