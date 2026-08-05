package top.chengdongqing.wechat.feature.intercom.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.core.network.audio.IntercomAudioEngine
import top.chengdongqing.wechat.feature.intercom.data.IntercomLanDiscovery
import top.chengdongqing.wechat.feature.intercom.service.IntercomForegroundService
import javax.inject.Inject

@HiltViewModel
class IntercomViewModel @Inject constructor(
    private val discovery: IntercomLanDiscovery,
    private val audioEngine: IntercomAudioEngine,
    connectionSettingsRepository: ConnectionSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val channels = discovery.channels
    val roomState = discovery.roomState
    val connectionMode = connectionSettingsRepository.connectionMode.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ConnectionMode.WiFiLan
    )

    init {
        viewModelScope.launch {
            connectionMode.collectLatest { mode ->
                audioEngine.stop()
                discovery.start(mode)
            }
        }
    }

    fun join(channelId: String) {
        discovery.join(channelId)
    }

    fun setSpeaking(isSpeaking: Boolean): Boolean {
        if (!audioEngine.setTransmitting(isSpeaking)) return false
        discovery.setSpeaking(isSpeaking)
        return true
    }

    fun enterRoom(channelId: String): Boolean {
        discovery.join(channelId)
        audioEngine.start(channelId, connectionMode.value)
        ContextCompat.startForegroundService(
            context,
            Intent(context, IntercomForegroundService::class.java)
                .setAction(IntercomForegroundService.ACTION_JOIN)
                .putExtra(IntercomForegroundService.EXTRA_CHANNEL, channelId)
                .putExtra(
                    IntercomForegroundService.EXTRA_CONNECTION_MODE,
                    connectionMode.value.name
                )
        )
        return true
    }

    fun canRecord() = audioEngine.canRecord()

    fun setPlaybackEnabled(enabled: Boolean) {
        audioEngine.setPlaybackEnabled(enabled)
    }

    fun leave() {
        audioEngine.stop()
        discovery.leave()
        context.stopService(Intent(context, IntercomForegroundService::class.java))
    }
}
