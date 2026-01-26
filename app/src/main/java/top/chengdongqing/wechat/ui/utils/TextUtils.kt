package top.chengdongqing.wechat.ui.utils

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import top.chengdongqing.wechat.data.sticker.Emojis

val EMOJI_PATTERN_REGEX = Regex("\\[(.*?)]")
val EMOJI_BACKSPACE_PATTERN_REGEX = Regex("\\[[^\\[\\]]+]$") // 以 [ 开头，中间包含非括号字符，以 ] 结尾，且必须紧贴末尾($)
val URL_PATTERN_REGEX = Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)")
val PHONE_PATTERN_REGEX = Regex("(\\d{3}-\\d{8}|\\d{11})")

/**
 * 文本解析
 */
fun String.parseRichText(
    onUrlClick: (String) -> Unit,
    onPhoneClick: (String) -> Unit
): AnnotatedString {
    val text = this

    return buildAnnotatedString {
        // 联合所有正则寻找匹配项
        val allMatches =
            (EMOJI_PATTERN_REGEX.findAll(text) + URL_PATTERN_REGEX.findAll(text) + PHONE_PATTERN_REGEX.findAll(
                text
            ))
                .sortedBy { it.range.first }

        var lastIndex = 0
        allMatches.forEach { match ->
            // 填充匹配项之前的普通文本
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }

            val matchText = match.value
            val start = length // 记录当前插入前的长度

            when {
                // 处理 Emoji
                matchText.startsWith("[") -> {
                    val emojiName = match.groupValues[1]
                    if (Emojis.any { it.description == emojiName }) {
                        appendInlineContent(id = emojiName, alternateText = matchText)
                    } else {
                        append(matchText)
                    }
                }
                // 处理 URL
                URL_PATTERN_REGEX.matches(matchText) -> {
                    append(matchText)
                    addLink(
                        url = LinkAnnotation.Url(
                            url = matchText,
                            styles = LinkStyles,
                            linkInteractionListener = { onUrlClick((it as LinkAnnotation.Url).url) }
                        ),
                        start = start,
                        end = length
                    )
                }
                // 处理电话号码
                else -> {
                    append(matchText)
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = "PHONE",
                            styles = LinkStyles,
                            linkInteractionListener = { onPhoneClick(matchText) }
                        ),
                        start = start,
                        end = length
                    )
                }
            }
            lastIndex = match.range.last + 1
        }

        // 填充剩余文本
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

private val LinkStyles = TextLinkStyles(
    style = SpanStyle(color = Color(0xFF576B95)),
    pressedStyle = SpanStyle(
        color = Color(0xFF576B95),
        background = Color(0x33576B95)
    )
)