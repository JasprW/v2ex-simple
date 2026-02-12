package im.fdx.v2ex.ui.member.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.member.MemberReplyModel
import im.fdx.v2ex.view.GoodTextView
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MemberRepliesRoute(
    username: String,
    onOpenTopic: (String) -> Unit,
    onRepliesCountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberRepliesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(username) {
        viewModel.loadIfNeeded(username)
    }

    LaunchedEffect(uiState.totalReplies) {
        if (uiState.totalReplies.isNotBlank()) {
            onRepliesCountChanged(uiState.totalReplies)
        }
    }

    MemberRepliesScreen(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() },
        onOpenTopic = onOpenTopic,
        modifier = modifier,
    )
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
fun MemberRepliesScreen(
    uiState: MemberRepliesUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx()
    }

    LaunchedEffect(listState, uiState.items.size, uiState.isLoading, uiState.isLoadingMore) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collectLatest { lastVisible ->
            val trigger = (uiState.items.size - 3).coerceAtLeast(0)
            if (lastVisible != null && lastVisible >= trigger) {
                onLoadMore()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.Transparent,
                    elevation = 0.dp,
                )
            },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = contentPullOffsetPx },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    items = uiState.items,
                ) { _, reply ->
                    MemberReplyItem(
                        reply = reply,
                        onClick = { onOpenTopic(reply.topic.id) },
                    )
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        if (uiState.isLoading && uiState.items.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MemberReplyItem(
    reply: MemberReplyModel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = reply.topic.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reply.createdOriginal,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!reply.content.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    GoodTextView(context)
                },
                update = { view ->
                    // TODO(compose-revamp): evaluate replacing GoodTextView with pure Compose rich text renderer.
                    view.setGoodText(reply.content, true)
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun MemberRepliesScreenPreview() {
    V2exTheme {
        MemberRepliesScreen(
            uiState = MemberRepliesUiState(
                username = "Livid",
                items = listOf(
                    MemberReplyModel(
                        id = "1",
                        topic = Topic(id = "100", title = "Compose 迁移最佳实践"),
                        content = "建议先拆 Route 和 Screen，再替换旧 Fragment。",
                        createdOriginal = "2 小时前",
                    ),
                    MemberReplyModel(
                        id = "2",
                        topic = Topic(id = "101", title = "Material 3 Expressive 组件选择"),
                        content = "Tab 建议用 PrimaryTabRow，文本用 titleMedium/bodyLarge。",
                        createdOriginal = "昨天",
                    ),
                ),
            ),
            onRefresh = {},
            onLoadMore = {},
            onOpenTopic = {},
        )
    }
}
