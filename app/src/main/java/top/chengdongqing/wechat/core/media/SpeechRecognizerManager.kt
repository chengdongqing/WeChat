package top.chengdongqing.wechat.core.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 语音识别状态
 */
enum class SpeechStatus {
    Idle,           // 空闲
    Listening,      // 正在听
    Processing,     // 识别中
    Error;           // 出错

    val isListening: Boolean get() = this == Listening
}

/**
 * 语音识别结果
 */
data class SpeechState(
    val status: SpeechStatus = SpeechStatus.Idle,
    /** 实时中间结果（说话过程中不断更新） */
    val partialResult: String = "",
    /** 最终识别结果 */
    val finalResult: String = "",
    /** 音量大小 0-10，驱动 UI 动画 */
    val volumeLevel: Float = 0f,
    /** 错误信息 */
    val errorMessage: String? = null
)

/**
 * 语音识别管理器
 *
 * 封装 Android [SpeechRecognizer]，提供 StateFlow 驱动的 API。
 *
 * 使用:
 * ```
 * val manager = SpeechRecognizerManager(context)
 *
 * // Compose 中订阅
 * val state by manager.state.collectAsStateWithLifecycle()
 *
 * // 开始/停止
 * manager.start()
 * manager.stop()
 *
 * // 页面销毁时释放
 * DisposableEffect(Unit) {
 *     onDispose { manager.destroy() }
 * }
 * ```
 *
 * 需要权限: android.permission.RECORD_AUDIO
 */
class SpeechRecognizerManager(private val context: Context) {

    private companion object {
        const val TAG = "SpeechRecognizer"
    }

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /**
     * 开始语音识别
     *
     * @param language 语言代码，默认中文。英文传 "en-US"
     */
    fun start(language: String = "zh-CN") {
        if (_state.value.status.isListening) return

        // 每次新建实例，避免复用导致的状态问题
        destroy()

        _state.value = SpeechState(status = SpeechStatus.Listening)

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // 识别模型: 自由形式（适合聊天输入）
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // 语言
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            // 开启中间结果（实时显示正在识别的文字）
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // 最多返回 1 个候选结果
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.startListening(intent)
        Log.d(TAG, "语音识别已启动, language=$language")
    }

    /**
     * 停止识别（会触发最终结果返回）
     */
    fun stop() {
        recognizer?.stopListening()
        Log.d(TAG, "语音识别已停止")
    }

    /**
     * 取消识别（不返回结果）
     */
    fun cancel() {
        recognizer?.cancel()
        _state.value = SpeechState(status = SpeechStatus.Idle)
        Log.d(TAG, "语音识别已取消")
    }

    /**
     * 释放资源，页面销毁时调用
     */
    fun destroy() {
        recognizer?.run {
            cancel()
            destroy()
        }
        recognizer = null
    }

    // ==================== RecognitionListener ====================

    private fun createListener() = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "准备就绪，请说话")
            _state.update { it.copy(status = SpeechStatus.Listening) }
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "检测到语音输入")
        }

        /**
         * 音量变化回调
         *
         * rmsdB 范围约 -2 到 10，归一化到 0-1 给 UI 做动画。
         */
        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _state.update { it.copy(volumeLevel = normalized) }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "语音输入结束，识别中...")
            _state.update { it.copy(status = SpeechStatus.Processing) }
        }

        override fun onError(error: Int) {
            val message = mapError(error)
            Log.e(TAG, "识别错误: $message (code=$error)")

            _state.update {
                it.copy(
                    status = SpeechStatus.Error,
                    errorMessage = message
                )
            }
        }

        /**
         * 最终结果
         */
        override fun onResults(results: Bundle?) {
            val texts = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            val result = texts?.firstOrNull() ?: ""
            Log.d(TAG, "最终结果: $result")

            _state.update {
                it.copy(
                    status = SpeechStatus.Idle,
                    finalResult = result,
                    partialResult = "",
                    volumeLevel = 0f
                )
            }
        }

        /**
         * 中间结果（实时更新）
         */
        override fun onPartialResults(partialResults: Bundle?) {
            val texts = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            val partial = texts?.firstOrNull() ?: return

            _state.update { it.copy(partialResult = partial) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ==================== 错误码映射 ====================

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "录音错误"
        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
        SpeechRecognizer.ERROR_NO_MATCH -> "未能识别，请重试"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务繁忙"
        SpeechRecognizer.ERROR_SERVER -> "服务端错误"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音"
        else -> "未知错误 ($error)"
    }
}