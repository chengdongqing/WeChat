package top.chengdongqing.wechat.feature.chat.ai

import java.io.File
import java.io.RandomAccessFile

data class LocalAiModelInfo(
    val name: String? = null,
    val description: String? = null,
    val architecture: String? = null,
    val parameterCount: Long? = null,
    val contextLength: Long? = null,
    val fileType: Int? = null
)

internal fun readGgufMetadata(file: File): LocalAiModelInfo? = runCatching {
    RandomAccessFile(file, "r").use { input ->
        require(input.readAscii(4) == "GGUF")
        val version = input.readUInt32()
        require(version in 2L..3L)
        input.readUInt64() // tensor count
        val metadataCount = input.readUInt64()
        require(metadataCount in 0..100_000)

        val values = mutableMapOf<String, Any?>()
        for (index in 0 until metadataCount.toInt()) {
            val key = input.readGgufString()
            // Tokenizer vocabulary/merge arrays can contain hundreds of thousands of
            // strings. All model-level fields we display precede them in standard GGUF
            // files, so never scan those arrays on the chat-info path.
            if (key.startsWith("tokenizer.")) break
            val type = input.readUInt32().toInt()
            val value =
                input.readValue(type, keep = key in INFO_KEYS || key.endsWith(".context_length"))
            if (value != null) values[key] = value
        }
        val architecture = values["general.architecture"] as? String
        LocalAiModelInfo(
            name = values["general.name"] as? String,
            description = values["general.description"] as? String,
            architecture = architecture,
            parameterCount = (values["general.parameter_count"] as? Number)?.toLong(),
            contextLength = architecture
                ?.let { values["$it.context_length"] as? Number }
                ?.toLong()
                ?: values.entries.firstOrNull { it.key.endsWith(".context_length") }
                    ?.value.let { it as? Number }?.toLong(),
            fileType = (values["general.file_type"] as? Number)?.toInt()
        )
    }
}.getOrNull()

private val INFO_KEYS = setOf(
    "general.name",
    "general.description",
    "general.architecture",
    "general.parameter_count",
    "general.file_type"
)

private fun RandomAccessFile.readValue(type: Int, keep: Boolean): Any? = when (type) {
    0 -> readUnsignedByte().takeIf { keep }
    1 -> readByte().takeIf { keep }
    2 -> readUInt16().takeIf { keep }
    3 -> readInt16().takeIf { keep }
    4 -> readUInt32().takeIf { keep }
    5 -> readInt32().takeIf { keep }
    6 -> readFloat32().takeIf { keep }
    7 -> (readUnsignedByte() != 0).takeIf { keep }
    8 -> readGgufString().takeIf { keep }
    9 -> {
        val elementType = readUInt32().toInt()
        val count = readUInt64()
        require(count in 0..10_000_000)
        val elementSize = elementType.fixedSize
        if (elementSize != null) {
            val bytes = Math.multiplyExact(count, elementSize.toLong())
            seek(Math.addExact(filePointer, bytes))
        } else {
            repeat(count.toInt()) { readValue(elementType, false) }
        }
        null
    }

    10 -> readUInt64().takeIf { keep }
    11 -> readInt64().takeIf { keep }
    12 -> readFloat64().takeIf { keep }
    else -> error("Unsupported GGUF metadata type: $type")
}

private val Int.fixedSize: Int?
    get() = when (this) {
        0, 1, 7 -> 1
        2, 3 -> 2
        4, 5, 6 -> 4
        10, 11, 12 -> 8
        else -> null
    }

private fun RandomAccessFile.readGgufString(): String {
    val length = readUInt64()
    require(length in 0..16_777_216)
    return ByteArray(length.toInt()).also(::readFully).decodeToString()
}

private fun RandomAccessFile.readAscii(length: Int) =
    ByteArray(length).also(::readFully).decodeToString()

private fun RandomAccessFile.readUInt16() =
    java.lang.Short.toUnsignedInt(java.lang.Short.reverseBytes(readShort()))

private fun RandomAccessFile.readInt16() = java.lang.Short.reverseBytes(readShort())
private fun RandomAccessFile.readUInt32() =
    Integer.toUnsignedLong(Integer.reverseBytes(readInt()))

private fun RandomAccessFile.readInt32() = Integer.reverseBytes(readInt())
private fun RandomAccessFile.readUInt64() = java.lang.Long.reverseBytes(readLong())
private fun RandomAccessFile.readInt64() = java.lang.Long.reverseBytes(readLong())
private fun RandomAccessFile.readFloat32() = Float.fromBits(readInt32())
private fun RandomAccessFile.readFloat64() = Double.fromBits(readInt64())
