package im.fdx.v2ex.ui.notification.compose

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import im.fdx.v2ex.R
import im.fdx.v2ex.model.NotificationModel
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.member.Member

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NotificationScreen(
    title: String,
    unreadCount: Int,
    notifications: List<NotificationModel>,
    isRefreshing: Boolean,
    isEmpty: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTopic: (NotificationModel) -> Unit,
    onOpenMember: (NotificationModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.topic_action_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.Transparent,
                    elevation = 0.dp,
                )
            },
        ) {
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.no_more_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = contentPullOffsetPx },
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(notifications) { index, item ->
                        NotificationItem(
                            model = item,
                            isUnread = unreadCount != -1 && index < unreadCount,
                            onOpenTopic = { onOpenTopic(item) },
                            onOpenMember = { onOpenMember(item) },
                        )
                        if (index < notifications.lastIndex) {
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
}

@Composable
private fun NotificationItem(
    model: NotificationModel,
    isUnread: Boolean,
    onOpenTopic: () -> Unit,
    onOpenMember: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTopic)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = model.member?.avatarNormalUrl,
                    contentDescription = stringResource(id = R.string.it_is_avatar),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenMember),
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.small),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = model.member?.username.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = model.time.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = notificationActionText(model.type.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = model.topic?.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!model.content.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = model.content.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun notificationActionText(originalType: String): String {
    return when {
        originalType.contains("感谢了你在主题") -> stringResource(id = R.string.notification_action_thanked)
        originalType.contains("回复了你") -> stringResource(id = R.string.notification_action_replied)
        originalType.contains("提到了你") -> stringResource(id = R.string.notification_action_mentioned)
        originalType.contains("收藏了你发布的主题") -> stringResource(id = R.string.notification_action_favorited)
        else -> originalType
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationScreenPreview() {
    V2exTheme {
        NotificationScreen(
            title = "Message (2)",
            unreadCount = 2,
            notifications = listOf(
                NotificationModel(
                    time = "2 分钟前",
                    type = "回复了你:",
                    topic = Topic(id = "1", title = "Compose migration"),
                    member = Member().apply { username = "Livid" },
                    content = "Looks great!",
                ),
            ),
            isRefreshing = false,
            isEmpty = false,
            onBack = {},
            onRefresh = {},
            onOpenTopic = {},
            onOpenMember = {},
        )
    }
}
