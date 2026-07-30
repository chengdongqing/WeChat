package top.chengdongqing.wechat.feature.chat.ai

import android.content.Context
import android.net.Uri
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.isModelLoaded
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LocalAiState {
    data object NoModel : LocalAiState
    data class Importing(val progressBytes: Long) : LocalAiState
    data object Loading : LocalAiState
    data object Cancelling : LocalAiState
    data class Ready(val modelName: String) : LocalAiState
    data class Error(val message: String) : LocalAiState
}

interface LocalAiEngine {
    val state: StateFlow<LocalAiState>
    val modelSizeBytes: Long?
    val modelInfo: LocalAiModelInfo?
    suspend fun importModel(uri: Uri)
    suspend fun cancelLoading()
    suspend fun unloadModel()
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
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<LocalAiState>(
        if (modelFile.isFile) LocalAiState.Loading else LocalAiState.NoModel
    )
    override val state: StateFlow<LocalAiState> = _state
    @Volatile
    private var cancelLoadingRequested = false

    private fun modelDisplayName(): String =
        nameFile.takeIf(File::isFile)?.readText()?.takeIf(String::isNotBlank) ?: modelFile.name

    override val modelSizeBytes: Long?
        get() = modelFile.takeIf(File::isFile)?.length()
    private var cachedModelSignature: Pair<Long, Long>? = null
    private var cachedModelInfo: LocalAiModelInfo? = null

    init {
        // A persisted selection is loaded immediately in a new process. This keeps
        // the public state model simple: Loading -> Ready, without an Unloaded state.
        if (modelFile.isFile) {
            engineScope.launch {
                runCatching { loadModel() }
                    .onFailure { _state.value = LocalAiState.Error(it.message ?: "模型加载失败") }
            }
        }
    }

    private fun deleteStoredModel() {
        modelFile.delete()
        nameFile.delete()
        File(modelDirectory, "xiaowei.backup").delete()
        File(modelDirectory, "xiaowei.importing").delete()
        cachedModelInfo = null
        cachedModelSignature = null
    }

    override val modelInfo: LocalAiModelInfo?
        get() {
            val file = modelFile.takeIf(File::isFile) ?: return null
            val signature = file.length() to file.lastModified()
            if (signature != cachedModelSignature) {
                cachedModelInfo = readGgufMetadata(file)
                cachedModelSignature = signature
            }
            return cachedModelInfo
        }

    override suspend fun importModel(uri: Uri) = withContext(Dispatchers.IO) {
        cancelLoadingRequested = false
        modelDirectory.mkdirs()
        val temporary = File(modelDirectory, "xiaowei.importing")
        runCatching {
            var copied = 0L
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取所选模型" }
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        check(!cancelLoadingRequested) { "已取消模型加载" }
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
            _state.value = if (it is CancellationException || cancelLoadingRequested) {
                deleteStoredModel()
                LocalAiState.NoModel
            } else {
                LocalAiState.Error(it.message ?: "模型导入失败")
            }
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
            if (cancelLoadingRequested) {
                engine.cleanUp()
                deleteStoredModel()
                _state.value = LocalAiState.NoModel
                return
            }
            engine.setSystemPrompt(
                "你是小微同学，一个友好、简洁、可靠的中文私人助手。" +
                    "所有推理都在用户设备本地完成。"
            )
        }
        _state.value = LocalAiState.Ready(
            modelDisplayName()
        )
    }

    override suspend fun cancelLoading() = withContext(Dispatchers.IO) {
        cancelLoadingRequested = true
        File(modelDirectory, "xiaowei.importing").delete()
        when (inference.state.value) {
            is InferenceEngine.State.LoadingModel -> {
                _state.value = LocalAiState.Cancelling
                return@withContext
            }

            is InferenceEngine.State.ModelReady -> inference.cleanUp()
            else -> Unit
        }
        deleteStoredModel()
        _state.value = LocalAiState.NoModel
    }

    override suspend fun unloadModel() = withContext(Dispatchers.IO) {
        cancelLoadingRequested = true
        val inferenceState = inference.state.value
        if (inferenceState.isModelLoaded || inferenceState is InferenceEngine.State.Error) {
            // cleanUp marks an active generation as cancelled, waits for the llama
            // dispatcher, then invokes the native unload routine.
            inference.cleanUp()
        }
        deleteStoredModel()
        _state.value = LocalAiState.NoModel
    }

    override fun generate(prompt: String): Flow<String> = flow {
        check(_state.value !is LocalAiState.Cancelling) { "模型正在取消加载，请稍候" }
        check(_state.value !is LocalAiState.NoModel) { "请先选择一个 GGUF 模型" }
        if (_state.value is LocalAiState.Loading) {
            val loadedState = state.first {
                it is LocalAiState.Ready ||
                        it is LocalAiState.Error ||
                        it is LocalAiState.NoModel
            }
            check(loadedState is LocalAiState.Ready) {
                (loadedState as? LocalAiState.Error)?.message ?: "模型加载失败"
            }
        }
        if (_state.value !is LocalAiState.Ready) {
            require(modelFile.isFile) { "请先选择一个 GGUF 模型" }
            cancelLoadingRequested = false
            loadModel()
        }
        inference.sendUserPrompt(prompt, predictLength = 512).collect { emit(it) }
    }
}
