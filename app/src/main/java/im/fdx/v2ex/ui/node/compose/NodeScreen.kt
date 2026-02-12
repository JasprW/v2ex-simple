package im.fdx.v2ex.ui.node.compose

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.compose.components.V2exTopicListItem
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.node.Node
import im.fdx.v2ex.ui.node.NodeUiState
import kotlinx.coroutines.flow.collectLatest

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NodeScreen(
    uiState: NodeUiState,
    showFollow: Boolean,
    showFab: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onTopicClick: (String) -> Unit,
    onNewTopic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx()
    }

    LaunchedEffect(listState, uiState.topics.size, uiState.isLoading, uiState.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { lastVisible ->
                val trigger = (uiState.topics.size - 3).coerceAtLeast(0)
                if (lastVisible != null && lastVisible >= trigger) {
                    onLoadMore()
                }
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.node?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.topic_action_back))
                    }
                },
                actions = {
                    if (showFollow) {
                        IconButton(onClick = onToggleFollow) {
                            Icon(
                                imageVector = if (uiState.isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (uiState.isFollowed) {
                                    stringResource(id = R.string.unfollow)
                                } else {
                                    stringResource(id = R.string.follow)
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = onNewTopic) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(id = R.string.new_topic))
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = pullToRefreshState,
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
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
                    .padding(top = 8.dp)
                    .graphicsLayer { translationY = contentPullOffsetPx },
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    NodeHeaderContent(node = uiState.node)
                }

                itemsIndexed(items = uiState.topics) { index, topic ->
                    V2exTopicListItem(topic = topic, onClick = { onTopicClick(topic.id) })
                    if (index < uiState.topics.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
}

@Composable
private fun NodeHeaderContent(node: Node?) {
    if (node == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = node.avatarLargeUrl,
            contentDescription = stringResource(id = R.string.node_image),
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "/${node.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    id = R.string.node_topics_count,
                    formatTopicsCount(node.topics),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!node.header.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = node.header.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun formatTopicsCount(count: Int): String {
    return if (count >= 1000) {
        "${count / 1000}k"
    } else {
        count.toString()
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeScreenPreview() {
    V2exTheme {
        NodeScreen(
            uiState = NodeUiState(
                nodeName = "android",
                node = Node(name = "android", title = "Android", topics = 42, header = "讨论 Android 开发"),
                topics = listOf(
                    Topic(id = "1", title = "Compose 重构分享", replies = 12, createdOriginal = "2 小时前"),
                ),
                isLoading = false,
            ),
            showFollow = true,
            showFab = true,
            onBack = {},
            onToggleFollow = {},
            onRefresh = {},
            onLoadMore = {},
            onTopicClick = {},
            onNewTopic = {},
        )
    }
}
