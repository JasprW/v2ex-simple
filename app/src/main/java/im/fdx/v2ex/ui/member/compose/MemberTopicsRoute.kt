package im.fdx.v2ex.ui.member.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Topic
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MemberTopicsRoute(
    username: String,
    avatar: String,
    onOpenTopic: (String) -> Unit,
    onTopicsCountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberTopicsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(username, avatar) {
        viewModel.loadIfNeeded(username = username, avatar = avatar)
    }

    LaunchedEffect(uiState.totalTopics) {
        if (uiState.totalTopics.isNotBlank()) {
            onTopicsCountChanged(uiState.totalTopics)
        }
    }

    MemberTopicsScreen(
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
fun MemberTopicsScreen(
    uiState: MemberTopicsUiState,
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
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { lastVisible ->
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
            if (!uiState.hiddenMessage.isNullOrBlank()) {
                Text(
                    text = uiState.hiddenMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(16.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = contentPullOffsetPx },
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(items = uiState.items) { index, topic ->
                        MemberTopicItem(
                            topic = topic,
                            onClick = { onOpenTopic(topic.id) },
                        )
                        if (index < uiState.items.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
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
        }

        if (uiState.isLoading && uiState.items.isEmpty() && uiState.hiddenMessage.isNullOrBlank()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MemberTopicItem(
    topic: Topic,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (topic.node?.title?.isNotBlank() == true) {
                Text(
                    text = topic.node?.title.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = topic.createdOriginal,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.member_reply_count, topic.replies ?: 0),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemberTopicsScreenPreview() {
    V2exTheme {
        MemberTopicsScreen(
            uiState = MemberTopicsUiState(
                username = "Livid",
                items = listOf(
                    Topic(id = "1", title = "Compose 重构路线", replies = 12, createdOriginal = "2 小时前"),
                    Topic(id = "2", title = "Material 3 typography 实践", replies = 5, createdOriginal = "昨天"),
                ),
            ),
            onRefresh = {},
            onLoadMore = {},
            onOpenTopic = {},
        )
    }
}
