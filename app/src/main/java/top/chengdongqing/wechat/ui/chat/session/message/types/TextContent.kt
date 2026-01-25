package top.chengdongqing.wechat.ui.chat.session.message.types

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.data.model.MessageContent

@Composable
fun TextContent(content: MessageContent.Text) {
    SelectionContainer {
        val annotatedString = remember(content.text) {
            parseTextWithLinksAndEmojis(content.text)
        }

        ClickableText(
            text = annotatedString,
            modifier = Modifier.padding(10.dp),
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.Black,
                lineHeight = 22.sp
            ),
            onClick = { offset ->
                // 处理链接点击
                annotatedString.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        // 逻辑：调用系统浏览器打开 URL
                        println("点击了链接: ${annotation.item}")
                    }

                annotatedString.getStringAnnotations("PHONE", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        // 逻辑：拨打电话
                        println("点击了电话: ${annotation.item}")
                    }
            }
        )
    }
}

/**
 * 将纯文本解析为带样式和链接的 AnnotatedString
 */
private fun parseTextWithLinksAndEmojis(text: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        // 正则匹配 URL
        val urlPattern = Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)")
        urlPattern.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(
                    color = Color(0xFF576B95),
                    textDecoration = TextDecoration.Underline
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
            addStringAnnotation(
                tag = "URL",
                annotation = match.value,
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 正则匹配电话号码
        val phonePattern = Regex("(\\d{3}-\\d{8}|\\d{11})")
        phonePattern.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF576B95)),
                start = match.range.first,
                end = match.range.last + 1
            )
            addStringAnnotation(
                tag = "PHONE",
                annotation = match.value,
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 3. TODO: 这里可以集成 Emoji 解析逻辑
        // 将 [微笑] 替换为 InlineContent 图标
    }
}