package im.fdx.v2ex.ui.topic.compose.richtext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun TopicRichText(
    html: String?,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onLinkClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val parsed = remember(html) { TopicHtmlParser.parse(html) }
    Column(modifier = modifier) {
        parsed.blocks.forEach { block ->
            when (block) {
                is RichBlock.Image -> {
                    AsyncImage(
                        model = block.url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onImageClick(parsed.images, block.index)
                            },
                        contentScale = ContentScale.FillWidth,
                    )
                }

                is RichBlock.Text -> {
                    val textStyle = when (block.style) {
                        RichTextStyle.BODY -> MaterialTheme.typography.bodyLarge
                        RichTextStyle.HEADING -> MaterialTheme.typography.titleLarge
                        RichTextStyle.QUOTE -> MaterialTheme.typography.bodyMedium
                        RichTextStyle.CODE -> MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    }

                    when (block.style) {
                        RichTextStyle.QUOTE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                LinkableText(
                                    value = block.value,
                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = maxLines,
                                    onLinkClick = onLinkClick,
                                )
                            }
                        }

                        RichTextStyle.CODE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                LinkableText(
                                    value = block.value,
                                    style = textStyle,
                                    maxLines = maxLines,
                                    onLinkClick = onLinkClick,
                                )
                            }
                        }

                        else -> {
                            LinkableText(
                                value = block.value,
                                style = textStyle,
                                maxLines = maxLines,
                                onLinkClick = onLinkClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkableText(
    value: androidx.compose.ui.text.AnnotatedString,
    style: TextStyle,
    maxLines: Int,
    onLinkClick: (String) -> Unit,
) {
    ClickableText(
        text = value,
        style = style.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.padding(vertical = 2.dp),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onClick = { offset ->
            val link = TopicHtmlParser.findLinkAtOffset(value, offset)
            if (link != null) {
                onLinkClick(link)
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TopicRichTextPreview() {
    val sample = """
        <h2>Compose Topic</h2>
        <p>这是一个 <a href=\"https://www.v2ex.com/t/1\">链接</a>，带有 <strong>粗体</strong> 和 <em>斜体</em>。</p>
        <blockquote>引用内容支持高亮展示。</blockquote>
        <pre><code>val answer = 42</code></pre>
    """.trimIndent()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Preview", style = MaterialTheme.typography.titleMedium)
        TopicRichText(
            html = sample,
            onLinkClick = {},
            onImageClick = { _, _ -> },
        )
    }
}
