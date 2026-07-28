package top.chengdongqing.wechat.feature.chat.ai

import android.content.Context
import android.net.Uri
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LocalAiState {
    data object NoModel : LocalAiState
    data class Importing(val progressBytes: Long) : LocalAiState
    data object Loading : LocalAiState
    data class Ready(val modelName: String) : LocalAiState
    data class Error(val message: String) : LocalAiState
}

interface LocalAiEngine {
    val state: StateFlow<LocalAiState>
    suspend fun importModel(uri: Uri)
    fun generate(prompt: String): Flow<String>
}

@Singleton
class LlamaCppLocalAiEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalAiEngine {
    private val modelDirectory = File(context.filesDir, "ai-models")
    private val modelFile = File(modelDirectory, "xiaowei.gguf")
    private val nameFile = File(modelDirectory, "xiaowei.name")
    private val inference by lazy { AiChat.getInferenceEngine(context) }
    private val _state = MutableStateFlow(
        if (modelFile.isFile) LocalAiState.Loading else LocalAiState.NoModel
    )
    override val state: StateFlow<LocalAiState> = _state

    override suspend fun importModel(uri: Uri) = withContext(Dispatchers.IO) {
        modelDirectory.mkdirs()
        val temporary = File(modelDirectory, "xiaowei.importing")
        runCatching {
            var copied = 0L
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取所选模型" }
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        _state.value = LocalAiState.Importing(copied)
                    }
                }
            }
            require(temporary.length() > 4L * 1024 * 1024) { "文件太小，不是有效的 GGUF 模型" }
            val header = temporary.inputStream().use {
                ByteArray(4).also(it::read)
            }.decodeToString()
            require(header == "GGUF") { "所选文件不是 GGUF 模型" }

            val backup = File(modelDirectory, "xiaowei.backup")
            if (backup.exists()) backup.delete()
            if (modelFile.exists()) {
                require(modelFile.renameTo(backup)) { "无法备份当前模型" }
            }
            if (!temporary.renameTo(modelFile)) {
                backup.renameTo(modelFile)
                error("保存模型失败")
            }
            val displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "本地模型"
            nameFile.writeText(displayName)
            runCatching { loadModel(forceReload = true) }
                .onFailure {
                    modelFile.delete()
                    backup.renameTo(modelFile)
                }
                .getOrThrow()
            backup.delete()
            Unit
        }.onFailure {
            temporary.delete()
            _state.value = LocalAiState.Error(it.message ?: "模型导入失败")
            throw it
        }.getOrThrow()
    }

    private suspend fun loadModel(forceReload: Boolean = false) {
        _state.value = LocalAiState.Loading
        val engine = inference
        engine.state.first {
            it is InferenceEngine.State.Initialized ||
                    it is InferenceEngine.State.ModelReady ||
                    it is InferenceEngine.State.Error
        }
        if (forceReload && engine.state.value is InferenceEngine.State.ModelReady) {
            engine.cleanUp()
        }
        if (engine.state.value !is InferenceEngine.State.ModelReady) {
            engine.loadModel(modelFile.absolutePath)
            engine.setSystemPrompt(
                "你是小微同学，一个友好、简洁、可靠的中文私人助手。" +
                    "所有推理都在用户设备本地完成。"
            )
        }
        _state.value = LocalAiState.Ready(
            nameFile.takeIf(File::isFile)?.readText()?.takeIf(String::isNotBlank)
                ?: modelFile.name
        )
    }

    override fun generate(prompt: String): Flow<String> = flow {
        if (_state.value !is LocalAiState.Ready) {
            require(modelFile.isFile) { "请先选择一个 GGUF 模型" }
            loadModel()
        }
        inference.sendUserPrompt(prompt, predictLength = 512).collect { emit(it) }
    }
}
