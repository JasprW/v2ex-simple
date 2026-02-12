package im.fdx.v2ex.ui.favor.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import im.fdx.v2ex.R
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.ui.compose.components.V2exTopicListItem
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.favor.FavorNodeUiState
import im.fdx.v2ex.ui.favor.FavorNodeViewModel
import im.fdx.v2ex.ui.favor.FavorTopicsUiState
import im.fdx.v2ex.ui.favor.FavorTopicsViewModel
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.node.Node

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavorScreen(
    onBack: () -> Unit,
    onOpenNode: (Node) -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onError: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.favor_tab_nodes),
        stringResource(id = R.string.favor_tab_topics),
        stringResource(id = R.string.favor_tab_following),
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.my_follow)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.topic_action_back))
                    }
                },
            )
        },
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> FavorNodesTab(onOpenNode = onOpenNode, onError = onError)
                1 -> FavorTopicsTab(
                    requestUrl = "${NetManager.HTTPS_V2EX_BASE}/my/topics",
                    onOpenTopic = onOpenTopic,
                    onError = onError,
                )

                else -> FavorTopicsTab(
                    requestUrl = NetManager.URL_FOLLOWING,
                    onOpenTopic = onOpenTopic,
                    onError = onError,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FavorNodesTab(
    onOpenNode: (Node) -> Unit,
    onError: (Int) -> Unit,
    viewModel: FavorNodeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load(onError = onError) }

    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx()
    }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.load(onError = onError) },
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
        if (uiState.isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(id = R.string.no_more_data), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = contentPullOffsetPx },
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.nodes, key = { it.name }) { node ->
                    FavorNodeCard(node = node, onClick = { onOpenNode(node) })
                }
            }
        }
    }
}

@Composable
private fun FavorNodeCard(
    node: Node,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            AsyncImage(
                model = node.avatarNormalUrl,
                contentDescription = stringResource(id = R.string.node_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "/${node.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (node.topics > 0) {
                Text(
                    text = stringResource(id = R.string.node_topics_count, node.topics.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FavorTopicsTab(
    requestUrl: String,
    onOpenTopic: (Topic) -> Unit,
    onError: (Int) -> Unit,
    viewModel: FavorTopicsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(requestUrl) { viewModel.loadIfNeeded(requestUrl, onError = onError) }
    LaunchedEffect(listState, uiState.items.size, uiState.isLoading, uiState.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                val trigger = (uiState.items.size - 3).coerceAtLeast(0)
                if (lastVisible != null && lastVisible >= trigger) {
                    viewModel.loadMore(onError = onError)
                }
            }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx()
    }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh(onError = onError) },
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
        if (uiState.isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(id = R.string.no_more_data), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = contentPullOffsetPx },
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(uiState.items) { index, topic ->
                    V2exTopicListItem(topic = topic, onClick = { onOpenTopic(topic) }, showNodeTitle = true)
                    if (index < uiState.items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavorScreenPreview() {
    V2exTheme {
        FavorScreen(
            onBack = {},
            onOpenNode = {},
            onOpenTopic = {},
            onError = {},
        )
    }
}
