package top.chengdongqing.wechat.ui.utils

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import top.chengdongqing.wechat.data.emoji.Emojis
import top.chengdongqing.wechat.ui.theme.LinkColor

// 提取配置常量
private object RichTextConfig {
    val EMOJI_PATTERN = Regex("\\[(.*?)]")
    val URL_PATTERN = Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)")
    val PHONE_PATTERN = Regex("(\\d{3}-\\d{8}|\\d{11})")

    val LinkStyles = TextLinkStyles(
        style = SpanStyle(LinkColor),
        pressedStyle = SpanStyle(
            color = LinkColor,
            background = LinkColor.copy(alpha = 0.1f)
        )
    )
}

/**
 * 解析富文本（增强版）
 */
fun String.parseRichText(
    onUrlClick: (String) -> Unit,
    onPhoneClick: (String) -> Unit
): AnnotatedString {
    return buildAnnotatedString {
        val matches = findAllRichTextMatches(this@parseRichText)
        var lastIndex = 0

        matches.forEach { match ->
            // 填充普通文本
            if (match.range.first > lastIndex) {
                append(this@parseRichText.substring(lastIndex, match.range.first))
            }

            val start = length

            when (match.type) {
                MatchType.Emoji -> {
                    val emojiName = match.text.substring(1, match.text.length - 1)
                    if (Emojis.findByDescription(emojiName) != null) {
                        appendInlineContent(id = emojiName, alternateText = match.text)
                    } else {
                        append(match.text)
                    }
                }

                MatchType.Url -> {
                    append(match.text)
                    addLink(
                        url = LinkAnnotation.Url(
                            url = match.text,
                            styles = RichTextConfig.LinkStyles,
                            linkInteractionListener = {
                                onUrlClick((it as LinkAnnotation.Url).url)
                            }
                        ),
                        start = start,
                        end = length
                    )
                }

                MatchType.Phone -> {
                    append(match.text)
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = "PHONE",
                            styles = RichTextConfig.LinkStyles,
                            linkInteractionListener = { onPhoneClick(match.text) }
                        ),
                        start = start,
                        end = length
                    )
                }
            }

            lastIndex = match.range.last + 1
        }

        // 填充剩余文本
        if (lastIndex < this@parseRichText.length) {
            append(this@parseRichText.substring(lastIndex))
        }
    }
}

/**
 * 匹配类型
 */
private enum class MatchType {
    Emoji, Url, Phone
}

/**
 * 匹配结果
 */
private data class RichTextMatch(
    val type: MatchType,
    val text: String,
    val range: IntRange
)

/**
 * 查找所有匹配项并处理重叠
 */
private fun findAllRichTextMatches(text: String): List<RichTextMatch> {
    val allMatches = mutableListOf<RichTextMatch>()

    // 查找所有表情
    RichTextConfig.EMOJI_PATTERN.findAll(text).forEach { match ->
        allMatches.add(
            RichTextMatch(
                type = MatchType.Emoji,
                text = match.value,
                range = match.range
            )
        )
    }

    // 查找所有 URL
    RichTextConfig.URL_PATTERN.findAll(text).forEach { match ->
        allMatches.add(
            RichTextMatch(
                type = MatchType.Url,
                text = match.value,
                range = match.range
            )
        )
    }

    // 查找所有电话
    RichTextConfig.PHONE_PATTERN.findAll(text).forEach { match ->
        allMatches.add(
            RichTextMatch(
                type = MatchType.Phone,
                text = match.value,
                range = match.range
            )
        )
    }

    // 按位置排序并移除重叠
    return allMatches
        .sortedBy { it.range.first }
        .fold(mutableListOf()) { acc, match ->
            if (acc.isEmpty() || acc.last().range.last < match.range.first) {
                acc.add(match)
            } else {
                // 发生重叠，保留优先级高的（表情 > URL > 电话）
                val last = acc.last()
                if (match.type.ordinal < last.type.ordinal) {
                    acc[acc.lastIndex] = match
                }
            }
            acc
        }
}