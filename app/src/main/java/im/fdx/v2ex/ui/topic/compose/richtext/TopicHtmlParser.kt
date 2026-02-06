package im.fdx.v2ex.ui.topic.compose.richtext

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import im.fdx.v2ex.utils.extensions.fullUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

private const val LINK_ANNOTATION = "link"

sealed interface RichBlock {
    data class Text(
        val value: AnnotatedString,
        val style: RichTextStyle,
    ) : RichBlock

    data class Image(
        val url: String,
        val index: Int,
    ) : RichBlock
}

enum class RichTextStyle {
    BODY,
    HEADING,
    QUOTE,
    CODE,
}

data class ParsedRichContent(
    val blocks: List<RichBlock>,
    val images: List<String>,
)

object TopicHtmlParser {

    fun parse(html: String?): ParsedRichContent {
        if (html.isNullOrBlank()) {
            return ParsedRichContent(emptyList(), emptyList())
        }
        val normalized = html.fullUrl()
        val document = Jsoup.parseBodyFragment(normalized)
        val blocks = mutableListOf<RichBlock>()
        val images = mutableListOf<String>()

        parseNodes(document.body().childNodes(), blocks, images)
        if (blocks.isEmpty()) {
            val fallback = document.body().text().trim()
            if (fallback.isNotEmpty()) {
                blocks += RichBlock.Text(AnnotatedString(fallback), RichTextStyle.BODY)
            }
        }

        return ParsedRichContent(blocks = blocks, images = images)
    }

    private fun parseNodes(
        nodes: List<Node>,
        blocks: MutableList<RichBlock>,
        images: MutableList<String>,
    ) {
        var inlineBuilder = AnnotatedString.Builder()

        fun flushInline(style: RichTextStyle = RichTextStyle.BODY) {
            val text = inlineBuilder.toAnnotatedString()
            if (text.text.isNotBlank()) {
                blocks += RichBlock.Text(text, style)
            }
            inlineBuilder = AnnotatedString.Builder()
        }

        nodes.forEach { node ->
            when (node) {
                is TextNode -> {
                    inlineBuilder.append(node.text())
                }

                is Element -> {
                    when (node.tagName()) {
                        "img" -> {
                            flushInline()
                            appendImage(node, blocks, images)
                        }

                        "br" -> inlineBuilder.append("\n")

                        "pre" -> {
                            flushInline()
                            val value = node.wholeText().ifBlank { node.text() }
                            if (value.isNotBlank()) {
                                blocks += RichBlock.Text(AnnotatedString(value), RichTextStyle.CODE)
                            }
                        }

                        "blockquote" -> {
                            flushInline()
                            val quoteBuilder = AnnotatedString.Builder()
                            node.childNodes().forEach { child -> appendInline(child, quoteBuilder) }
                            val quote = quoteBuilder.toAnnotatedString()
                            if (quote.text.isNotBlank()) {
                                blocks += RichBlock.Text(quote, RichTextStyle.QUOTE)
                            }
                        }

                        "ul", "ol" -> {
                            flushInline()
                            node.children().forEachIndexed { index, li ->
                                val liBuilder = AnnotatedString.Builder()
                                val prefix = if (node.tagName() == "ol") "${index + 1}. " else "• "
                                liBuilder.append(prefix)
                                li.childNodes().forEach { child -> appendInline(child, liBuilder) }
                                val item = liBuilder.toAnnotatedString()
                                if (item.text.isNotBlank()) {
                                    blocks += RichBlock.Text(item, RichTextStyle.BODY)
                                }
                            }
                        }

                        "h1", "h2", "h3", "h4" -> {
                            flushInline()
                            val headingBuilder = AnnotatedString.Builder()
                            node.childNodes().forEach { child -> appendInline(child, headingBuilder) }
                            val heading = headingBuilder.toAnnotatedString()
                            if (heading.text.isNotBlank()) {
                                blocks += RichBlock.Text(heading, RichTextStyle.HEADING)
                            }
                        }

                        "p", "div", "section", "article", "span" -> {
                            if (hasBlockChild(node)) {
                                flushInline()
                                parseNodes(node.childNodes(), blocks, images)
                            } else {
                                node.childNodes().forEach { child -> appendInline(child, inlineBuilder) }
                                flushInline()
                            }
                        }

                        else -> {
                            if (node.tagName() == "a" || node.tagName() == "code" || node.tagName() == "strong" || node.tagName() == "em") {
                                appendInline(node, inlineBuilder)
                            } else if (hasBlockChild(node)) {
                                flushInline()
                                parseNodes(node.childNodes(), blocks, images)
                            } else {
                                node.childNodes().forEach { child -> appendInline(child, inlineBuilder) }
                            }
                        }
                    }
                }
            }
        }

        flushInline()
    }

    private fun appendImage(
        element: Element,
        blocks: MutableList<RichBlock>,
        images: MutableList<String>,
    ) {
        val url = element.attr("src").trim()
        if (url.isBlank()) {
            return
        }
        val fixedUrl = if (url.startsWith("//")) {
            "https:$url"
        } else {
            url
        }
        val index = images.size
        images += fixedUrl
        blocks += RichBlock.Image(url = fixedUrl, index = index)
    }

    private fun appendInline(node: Node, builder: AnnotatedString.Builder) {
        when (node) {
            is TextNode -> builder.append(node.text())
            is Element -> {
                when (node.tagName()) {
                    "br" -> builder.append("\n")
                    "img" -> Unit
                    "a" -> {
                        val href = node.attr("href").trim()
                        if (href.isBlank()) {
                            node.childNodes().forEach { child -> appendInline(child, builder) }
                        } else {
                            val normalizedHref = if (href.startsWith("//")) "https:$href" else href
                            builder.pushStringAnnotation(tag = LINK_ANNOTATION, annotation = normalizedHref)
                            builder.pushStyle(
                                SpanStyle(
                                    color = androidx.compose.ui.graphics.Color(0xFF2F80ED),
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            node.childNodes().forEach { child -> appendInline(child, builder) }
                            builder.pop()
                            builder.pop()
                        }
                    }

                    "strong", "b" -> {
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        node.childNodes().forEach { child -> appendInline(child, builder) }
                        builder.pop()
                    }

                    "em", "i" -> {
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        node.childNodes().forEach { child -> appendInline(child, builder) }
                        builder.pop()
                    }

                    "code" -> {
                        builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                        node.childNodes().forEach { child -> appendInline(child, builder) }
                        builder.pop()
                    }

                    else -> node.childNodes().forEach { child -> appendInline(child, builder) }
                }
            }
        }
    }

    private fun hasBlockChild(element: Element): Boolean {
        return element.children().any { child ->
            child.tagName() in setOf("img", "pre", "blockquote", "ul", "ol", "h1", "h2", "h3", "h4", "p", "div")
        }
    }

    fun findLinkAtOffset(text: AnnotatedString, offset: Int): String? {
        return text.getStringAnnotations(tag = LINK_ANNOTATION, start = offset, end = offset)
            .firstOrNull()
            ?.item
    }
}
