package top.chengdongqing.wechat.feature.profile.ui.favorites

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FavoriteAttachment(
    val id: String,
    val kind: Kind,
    val path: String = "",
    val mimeType: String = "",
    val displayName: String = "",
    val durationMs: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val address: String = "",
    val mapUri: String = ""
) {
    @Serializable
    enum class Kind { IMAGE, VIDEO, AUDIO, FILE, LOCATION }
}

private const val ATTACHMENT_PREFIX = "attachments-v2:"
private val attachmentJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeFavoriteAttachments(items: List<FavoriteAttachment>): String =
    if (items.isEmpty()) "" else ATTACHMENT_PREFIX + attachmentJson.encodeToString(items)

fun decodeFavoriteAttachments(value: String): List<FavoriteAttachment> {
    if (value.startsWith(ATTACHMENT_PREFIX)) {
        return runCatching {
            attachmentJson.decodeFromString<List<FavoriteAttachment>>(
                value.removePrefix(ATTACHMENT_PREFIX)
            )
        }.getOrDefault(emptyList())
    }
    return value.lineSequence().filter(String::isNotBlank).map { path ->
        val extension = File(path).extension.lowercase()
        val kind = when (extension) {
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif" ->
                FavoriteAttachment.Kind.IMAGE

            "mp4", "mov", "mkv", "avi", "3gp", "webm" ->
                FavoriteAttachment.Kind.VIDEO

            "m4a", "aac", "mp3", "wav", "ogg", "amr" ->
                FavoriteAttachment.Kind.AUDIO

            else -> FavoriteAttachment.Kind.FILE
        }
        FavoriteAttachment(
            id = path,
            kind = kind,
            path = path,
            displayName = File(path).name
        )
    }.toList()
}

fun List<FavoriteAttachment>.primaryType(hasText: Boolean): String {
    if (hasText || size != 1) return "RICH_TEXT"
    return when (first().kind) {
        FavoriteAttachment.Kind.AUDIO -> "VOICE"
        FavoriteAttachment.Kind.LOCATION -> "LOCATION"
        FavoriteAttachment.Kind.IMAGE, FavoriteAttachment.Kind.VIDEO -> "MEDIA"
        FavoriteAttachment.Kind.FILE -> "FILE"
    }
}
