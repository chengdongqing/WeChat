package top.chengdongqing.wechat.core.common.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.annotation.RawRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.lang.ref.WeakReference
import top.chengdongqing.wechat.core.designsystem.R as DesignR

/**
 * 封装 MediaPlayer 生命周期
 */
class MusicPlayer(context: Context) {

    private val context = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager =
        this.context.getSystemService(NotificationManager::class.java)
    private val mediaSession = MediaSession(this.context, "WeChatMusicPlayer").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() = play()
            override fun onPause() = pause()
            override fun onSeekTo(pos: Long) = seekToMillis(pos)
        })
        isActive = true
    }
    private var title = ""
    private var artist = ""
    private var albumArt: Bitmap? = null
    private var lastNotificationSecond = -1

    init {
        activePlayerReference = WeakReference(this)
    }

    /** 当前播放进度 0f~1f */
    var progress by mutableFloatStateOf(0f)
        private set

    /** 是否正在播放 */
    var isPlaying by mutableStateOf(false)
        private set

    /** 总时长（毫秒），prepare 完成后才有值 */
    var duration by mutableIntStateOf(0)
        private set

    /**
     * 设置系统媒体卡片显示的歌曲信息。
     * [albumArtModel] 支持 drawable 资源 ID、文件路径和图片字节数组。
     */
    fun setMetadata(title: String, artist: String, albumArtModel: Any?) {
        this.title = title
        this.artist = artist
        albumArt = when (albumArtModel) {
            is Int -> albumArtModel.takeIf { it != 0 }?.let {
                BitmapFactory.decodeResource(context.resources, it)
            }
            is String -> BitmapFactory.decodeFile(albumArtModel)
            is ByteArray -> BitmapFactory.decodeByteArray(albumArtModel, 0, albumArtModel.size)
            else -> null
        }
        updateMediaSession()
    }

    /**
     * 加载并准备音频资源
     */
    fun prepare(@RawRes resId: Int) {
        resetMediaPlayer()
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            this@MusicPlayer.duration = duration
            // 播放完毕后重置状态
            setOnCompletionListener {
                this@MusicPlayer.isPlaying = false
                this@MusicPlayer.progress = 0f
                updateMediaSession()
                showNotification()
            }
        }
        updateMediaSession()
    }

    fun prepare(path: String) {
        resetMediaPlayer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            this@MusicPlayer.duration = duration
            setOnCompletionListener {
                this@MusicPlayer.isPlaying = false
                this@MusicPlayer.progress = 0f
                updateMediaSession()
                showNotification()
            }
        }
        updateMediaSession()
    }

    fun play() {
        mediaPlayer?.start()
        isPlaying = true
        updateMediaSession()
        showNotification()
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
        updateMediaSession()
        showNotification()
    }

    fun togglePlay() {
        if (isPlaying) pause() else play()
    }

    /**
     * 跳转到指定进度
     * @param fraction 0f~1f
     */
    fun seekTo(fraction: Float) {
        mediaPlayer?.let {
            it.seekTo((fraction * it.duration).toInt())
            progress = fraction
            updateMediaSession()
            showNotification()
        }
    }

    private fun seekToMillis(position: Long) {
        mediaPlayer?.let {
            it.seekTo(position.coerceIn(0, it.duration.toLong()).toInt())
            progress = if (it.duration > 0) position.toFloat() / it.duration else 0f
            updateMediaSession()
            showNotification()
        }
    }

    /**
     * 刷新当前播放进度
     */
    fun updateProgress() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying && mp.duration > 0) {
                progress = mp.currentPosition.toFloat() / mp.duration
                updateMediaSession()
                val currentSecond = mp.currentPosition / 1_000
                if (currentSecond != lastNotificationSecond) {
                    lastNotificationSecond = currentSecond
                    showNotification()
                }
            }
        }
    }

    private fun updateMediaSession() {
        val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        mediaSession.setMetadata(
            android.media.MediaMetadata.Builder()
                .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, duration.toLong())
                .apply {
                    albumArt?.let {
                        putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                        putBitmap(android.media.MediaMetadata.METADATA_KEY_ART, it)
                    }
                }
                .build()
        )
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SEEK_TO
                )
                .setState(state, position, if (isPlaying) 1f else 0f)
                .build()
        )
    }

    private fun showNotification() {
        createNotificationChannel()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val toggleIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, MusicNotificationActionReceiver::class.java).apply {
                action = Intent.ACTION_MEDIA_BUTTON
                putExtra(
                    Intent.EXTRA_KEY_EVENT,
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    )
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = context.applicationInfo.icon
        val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(albumArt)
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setProgress(duration, mediaPlayer?.currentPosition ?: 0, duration <= 0)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        context,
                        if (isPlaying) DesignR.drawable.ic_pause_filled
                        else DesignR.drawable.ic_play_filled
                    ),
                    if (isPlaying) "暂停" else "播放",
                    toggleIntent
                ).build()
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "音乐播放",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "显示正在播放的歌曲和播放控制"
                    setShowBadge(false)
                }
            )
        }
    }

    /**
     * 释放 MediaPlayer 资源
     */
    fun release() {
        resetMediaPlayer()
        notificationManager.cancel(NOTIFICATION_ID)
        mediaSession.isActive = false
        mediaSession.release()
        if (activePlayerReference.get() === this) {
            activePlayerReference.clear()
        }
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        progress = 0f
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "music_playback"
        const val NOTIFICATION_ID = 2_002
        internal var activePlayerReference = WeakReference<MusicPlayer>(null)
    }
}

/**
 * 接收通知栏的播放/暂停操作。播放器本身仍由播放器页面持有和释放。
 */
class MusicNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
            MusicPlayer.activePlayerReference.get()?.togglePlay()
        }
    }
}
