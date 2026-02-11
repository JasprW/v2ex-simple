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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
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
import im.fdx.v2ex.ui.member.Member
import im.fdx.v2ex.ui.member.MemberTopicsUiState
import im.fdx.v2ex.ui.node.Node

/**
 * Member 主题列表内容
 */
@Composable
fun MemberTopicsContent(
    state: MemberTopicsUiState,
    onTopicClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (state) {
            is MemberTopicsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MemberTopicsUiState.Error -> {
                V2exErrorView(
                    message = state.message,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is MemberTopicsUiState.Success -> {
                if (state.topics.isEmpty()) {
                    V2exEmptyView(
                        message = "暂无主题",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            items = state.topics,
                            key = { it.id }
                        ) { topic ->
                            TopicItem(
                                topic = topic,
                                onClick = { onTopicClick(topic.id) }
                            )
                            HorizontalDivider()
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
private fun TopicItem(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = "${topic.node?.title ?: ""} · ${topic.member?.username ?: ""} · ${topic.showCreated()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val replies = topic.replies
                if (replies != null && replies > 0) {
                    Text(
                        text = "$replies 回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
private fun MemberTopicsContentPreview() {
    V2exTheme {
        MemberTopicsContent(
            state = MemberTopicsUiState.Success(
                topics = listOf(
                    Topic(
                        id = "1",
                        title = "这是一个测试主题标题",
                        content = "内容",
                        node = Node("node1", "技术"),
                        member = Member(id = "1", username = "testuser"),
                        replies = 10,
                        created = System.currentTimeMillis()
                    ),
                    Topic(
                        id = "2",
                        title = "这是另一个测试主题标题，稍微长一点",
                        content = "内容",
                        node = Node("node2", "生活"),
                        member = Member(id = "2", username = "testuser2"),
                        replies = 5,
                        created = System.currentTimeMillis()
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
private fun MemberTopicsContentLoadingPreview() {
    V2exTheme {
        MemberTopicsContent(
            state = MemberTopicsUiState.Loading,
            onTopicClick = {},
            onRefresh = {},
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun MemberTopicsContentEmptyPreview() {
    V2exTheme {
        MemberTopicsContent(
            state = MemberTopicsUiState.Success(
                topics = emptyList(),
                currentPage = 1,
                totalPages = 1
            ),
            onTopicClick = {},
            onRefresh = {},
            onLoadMore = {}
        )
    }
}
