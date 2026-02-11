package im.fdx.v2ex.ui.member.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import im.fdx.v2ex.ui.compose.components.V2exEmptyView
import im.fdx.v2ex.ui.compose.components.V2exErrorView
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.member.MemberReplyModel
import im.fdx.v2ex.ui.member.MemberRepliesUiState

/**
 * Member 回复列表内容
 */
@Composable
fun MemberRepliesContent(
    state: MemberRepliesUiState,
    onTopicClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (state) {
            is MemberRepliesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MemberRepliesUiState.Error -> {
                V2exErrorView(
                    message = state.message,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is MemberRepliesUiState.Success -> {
                if (state.replies.isEmpty()) {
                    V2exEmptyView(
                        message = "暂无回复",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.replies,
                            key = { it.id ?: it.hashCode().toString() }
                        ) { reply ->
                            ReplyItem(
                                reply = reply,
                                onClick = { onTopicClick(reply.topic.id) }
                            )
                        }

                        // 加载更多指示器
                        if (state.currentPage < state.totalPages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                                onLoadMore()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(
    reply: MemberReplyModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 回复的主题标题
            Text(
                text = "${reply.createdOriginal} 回复了主题：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = reply.topic.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 回复内容（纯文本显示）
            val plainContent = reply.content
                ?.replace(Regex("<[^>]*>"), "")
                ?.replace("&quot;", "\"")
                ?.replace("&amp;", "&")
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?: ""

            Text(
                text = plainContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemberRepliesContentPreview() {
    V2exTheme {
        MemberRepliesContent(
            state = MemberRepliesUiState.Success(
                replies = listOf(
                    MemberReplyModel(
                        id = "1",
                        topic = Topic(
                            id = "topic1",
                            title = "这是一个测试主题",
                            content = "",
                            created = System.currentTimeMillis()
                        ),
                        content = "这是一条回复内容，说得很有道理。",
                        createdOriginal = "2024-01-15 10:30"
                    ),
                    MemberReplyModel(
                        id = "2",
                        topic = Topic(
                            id = "topic2",
                            title = "这是另一个测试主题，标题比较长",
                            content = "",
                            created = System.currentTimeMillis()
                        ),
                        content = "这是另一条回复，内容也比较长一些，用来测试文本截断效果。",
                        createdOriginal = "2024-01-14 15:20"
                    )
                ),
                currentPage = 1,
                totalPages = 3
            ),
            onTopicClick = {},
            onRefresh = {},
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun MemberRepliesContentLoadingPreview() {
    V2exTheme {
        MemberRepliesContent(
            state = MemberRepliesUiState.Loading,
            onTopicClick = {},
            onRefresh = {},
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun MemberRepliesContentEmptyPreview() {
    V2exTheme {
        MemberRepliesContent(
            state = MemberRepliesUiState.Success(
                replies = emptyList(),
                currentPage = 1,
                totalPages = 1
            ),
            onTopicClick = {},
            onRefresh = {},
            onLoadMore = {}
        )
    }
}
