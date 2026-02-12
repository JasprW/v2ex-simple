package im.fdx.v2ex.ui.topic.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.compose.AsyncImage
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Comment
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.member.Member
import im.fdx.v2ex.ui.node.Node
import im.fdx.v2ex.ui.topic.Reply
import im.fdx.v2ex.ui.topic.TopicDetailEffect
import im.fdx.v2ex.ui.topic.TopicDetailUiState
import im.fdx.v2ex.ui.topic.TopicDetailViewModel
import im.fdx.v2ex.ui.topic.compose.richtext.TopicRichText
import im.fdx.v2ex.utils.Keys
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class UserRepliesSheet(
    val title: String,
    val replies: List<Reply>,
)

@Composable
fun TopicDetailRoute(
    topicId: String,
    initialTopic: Topic?,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenMember: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenPhotos: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TopicDetailViewModel = viewModel(
        key = "topic-detail-$topicId",
        factory = TopicDetailViewModel.factory(
            topicId = topicId,
            initialTopic = initialTopic,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.persistDraft()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is TopicDetailEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                TopicDetailEffect.RequestLogin -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.not_login_tips),
                        actionLabel = context.getString(R.string.login),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        onLogin()
                    }
                }

                is TopicDetailEffect.ShareText -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }

                is TopicDetailEffect.OpenBrowser -> {
                    val uri = Uri.parse(effect.url)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    if (effect.preferChrome) {
                        intent.`package` = "com.android.chrome"
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        if (effect.preferChrome) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    }
                }

                is TopicDetailEffect.OpenMember -> onOpenMember(effect.username)
                is TopicDetailEffect.OpenNode -> onOpenNode(effect.nodeName)
                is TopicDetailEffect.OpenTopic -> onOpenTopic(effect.topicId)
                is TopicDetailEffect.OpenPhotos -> onOpenPhotos(effect.photos, effect.position)
                is TopicDetailEffect.TopicIgnored -> {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                        Intent(Keys.ACTION_HIDE_TOPIC).apply {
                            putExtra(Keys.KEY_TOPIC_ID, effect.topicId)
                        },
                    )
                    snackbarHostState.showSnackbar(
                        if (effect.ignored) {
                            context.getString(R.string.topic_block_success)
                        } else {
                            context.getString(R.string.topic_unblock_success)
                        },
                    )
                    onBack()
                }

                TopicDetailEffect.ScrollToBottom -> {
                    val target = uiState.replies.size
                    if (target > 0) {
                        listState.animateScrollToItem(target)
                    }
                }
            }
        }
    }

    TopicDetailScreen(
        uiState = uiState,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = { viewModel.refresh() },
        onLoadMore = viewModel::loadNextPage,
        onFavor = viewModel::onFavorClick,
        onThankTopic = viewModel::onThankTopicClick,
        onIgnoreTopic = viewModel::onIgnoreTopicClick,
        onShare = viewModel::onShareTopicClick,
        onOpenInBrowser = viewModel::onOpenInBrowserClick,
        onReply = { viewModel.onReplyClick() },
        onReplyTo = viewModel::onReplyTo,
        onReplyTextChange = viewModel::onComposerTextChange,
        onReplyDismiss = viewModel::onComposerDismiss,
        onReplySend = viewModel::onComposerSend,
        onReportTopic = viewModel::onReportTopic,
        onReportReply = viewModel::onReportReply,
        onThankReply = viewModel::onThankReply,
        onHideReply = viewModel::onHideReply,
        onAvatarClick = viewModel::onAvatarClick,
        onNodeClick = viewModel::onNodeClick,
        onHtmlLinkClick = viewModel::onHtmlLinkClick,
        onImageClick = viewModel::onRichImageClick,
        modifier = modifier,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun TopicDetailScreen(
    uiState: TopicDetailUiState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFavor: () -> Unit,
    onThankTopic: () -> Unit,
    onIgnoreTopic: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onReply: () -> Unit,
    onReplyTo: (Reply, Int) -> Unit,
    onReplyTextChange: (String) -> Unit,
    onReplyDismiss: () -> Unit,
    onReplySend: () -> Unit,
    onReportTopic: (String) -> Unit,
    onReportReply: (Reply, Int, String) -> Unit,
    onThankReply: (Reply) -> Unit,
    onHideReply: (Reply) -> Unit,
    onAvatarClick: (String) -> Unit,
    onNodeClick: (String) -> Unit,
    onHtmlLinkClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val blockLabel = stringResource(R.string.block)
    val unblockLabel = stringResource(R.string.cancel_block)
    val conversationTitle = stringResource(R.string.topic_conversation_title)
    val reportReasons = stringArrayResource(R.array.topic_report_reasons).toList()
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var showTopMenu by remember { mutableStateOf(false) }
    var showTopicReportSheet by remember { mutableStateOf(false) }
    var replyActionTarget by remember { mutableStateOf<Reply?>(null) }
    var replyReportTarget by remember { mutableStateOf<Reply?>(null) }
    var userRepliesSheet by remember { mutableStateOf<UserRepliesSheet?>(null) }
    var showActionDockOnScroll by remember { mutableStateOf(true) }
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItemOffset by remember { mutableIntStateOf(0) }

    val showCollapsedTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
        }
    }
    val showActionDock = !uiState.composerVisible && showActionDockOnScroll
    val expressiveMotion = remember { MotionScheme.expressive() }
    val snackbarBottomPadding by animateDpAsState(
        targetValue = if (showActionDock) 48.dp else 16.dp,
        label = "topic_snackbar_bottom_padding",
    )

    LaunchedEffect(uiState.canLoadMore, uiState.replies.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
            .map { lastIndex ->
                val totalItems = uiState.replies.size + 1
                uiState.canLoadMore && lastIndex >= totalItems - 3
            }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad) {
                    onLoadMore()
                }
            }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                val isScrollingDown =
                    index > previousFirstVisibleItemIndex ||
                        (index == previousFirstVisibleItemIndex &&
                            offset > previousFirstVisibleItemOffset)
                val isScrollingUp =
                    index < previousFirstVisibleItemIndex ||
                        (index == previousFirstVisibleItemIndex &&
                            offset < previousFirstVisibleItemOffset)
                if (isScrollingDown) {
                    showActionDockOnScroll = false
                } else if (isScrollingUp) {
                    showActionDockOnScroll = true
                }
                previousFirstVisibleItemIndex = index
                previousFirstVisibleItemOffset = offset
            }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        (pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx())
            .coerceAtLeast(0f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = snackbarBottomPadding),
                )
            },
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    title = {
                        AnimatedVisibility(
                            visible = showCollapsedTitle,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Text(
                                text = uiState.topic?.title.orEmpty(),
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_primary_24dp),
                                contentDescription = stringResource(R.string.topic_action_back),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.topic_action_more))
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (uiState.isIgnored) {
                                            unblockLabel
                                        } else {
                                            blockLabel
                                        },
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    onIgnoreTopic()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.report_abuse)) },
                                onClick = {
                                    showTopMenu = false
                                    showTopicReportSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_open_in_browser)) },
                                onClick = {
                                    showTopMenu = false
                                    onOpenInBrowser()
                                },
                            )
                        }
                    },
                )
            },
        ) { padding ->
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = uiState.isRefreshing,
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
                    contentPadding = PaddingValues(bottom = 110.dp),
                ) {
                    item(key = "topic-header") {
                        TopicHeader(
                            topic = uiState.topic,
                            onAvatarClick = onAvatarClick,
                            onNodeClick = onNodeClick,
                            onHtmlLinkClick = onHtmlLinkClick,
                            onImageClick = onImageClick,
                        )
                    }

                    itemsIndexed(
                        items = uiState.replies,
                    ) { index, reply ->
                        ReplyCard(
                            reply = reply,
                            rowNum = reply.getRowNum(index + 1),
                            onAvatarClick = onAvatarClick,
                            onReply = { onReplyTo(reply, reply.getRowNum(index + 1)) },
                            onThank = { onThankReply(reply) },
                            onMore = { replyActionTarget = reply },
                            onHtmlLinkClick = onHtmlLinkClick,
                            onImageClick = onImageClick,
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            visible = showActionDock,
            enter =
                fadeIn(animationSpec = expressiveMotion.defaultEffectsSpec()) +
                    slideInVertically(
                        animationSpec = expressiveMotion.slowSpatialSpec(),
                        initialOffsetY = { it },
                    ) +
                    scaleIn(
                        animationSpec = expressiveMotion.defaultSpatialSpec(),
                        initialScale = 0.9f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                    ),
            exit =
                fadeOut(animationSpec = expressiveMotion.fastEffectsSpec()) +
                    slideOutVertically(
                        animationSpec = expressiveMotion.fastSpatialSpec(),
                        targetOffsetY = { it },
                    ) +
                    scaleOut(
                        animationSpec = expressiveMotion.fastSpatialSpec(),
                        targetScale = 0.92f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                    ),
        ) {
            TopicActionDock(
                favored = uiState.isFavored,
                thanked = uiState.isThanked,
                onFavor = onFavor,
                onThank = onThankTopic,
                onReply = onReply,
                onShare = onShare,
            )
        }
    }

    if (showTopicReportSheet) {
        ReportReasonSheet(
            reasons = reportReasons,
            onDismiss = { showTopicReportSheet = false },
            onSelect = { reason ->
                showTopicReportSheet = false
                onReportTopic(reason)
            },
        )
    }

    replyActionTarget?.let { reply ->
        ReplyActionSheet(
            onDismiss = { replyActionTarget = null },
            onReply = {
                replyActionTarget = null
                onReplyTo(reply, reply.getRowNum().takeIf { it > 0 } ?: 1)
            },
            onThank = {
                replyActionTarget = null
                onThankReply(reply)
            },
            onHide = {
                replyActionTarget = null
                onHideReply(reply)
            },
            onReport = {
                replyActionTarget = null
                replyReportTarget = reply
            },
            onCopy = {
                replyActionTarget = null
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(context.getString(R.string.reply), reply.content),
                )
                snackbarHostState.currentSnackbarData?.dismiss()
            },
            onShowUserReplies = {
                replyActionTarget = null
                val username = reply.member?.username.orEmpty()
                val list = uiState.replies.filter { it.member?.username == username }
                userRepliesSheet = UserRepliesSheet(
                    title = username,
                    replies = list,
                )
            },
            onShowConversation = {
                replyActionTarget = null
                val list = buildConversationReplies(uiState.replies, reply)
                userRepliesSheet = UserRepliesSheet(
                    title = conversationTitle,
                    replies = list,
                )
            },
        )
    }

    replyReportTarget?.let { reply ->
        ReportReasonSheet(
            reasons = reportReasons,
            onDismiss = { replyReportTarget = null },
            onSelect = { reason ->
                val rowNum = reply.getRowNum()
                onReportReply(reply, rowNum, reason)
                replyReportTarget = null
            },
        )
    }

    userRepliesSheet?.let { sheet ->
        UserRepliesBottomSheet(
            title = sheet.title,
            replies = sheet.replies,
            onDismiss = { userRepliesSheet = null },
            onAvatarClick = onAvatarClick,
            onReply = { reply, rowNum ->
                userRepliesSheet = null
                onReplyTo(reply, rowNum)
            },
            onThank = onThankReply,
            onMore = { replyActionTarget = it },
            onHtmlLinkClick = onHtmlLinkClick,
            onImageClick = onImageClick,
        )
    }

    if (uiState.composerVisible) {
        ReplyComposerSheet(
            value = uiState.composerText,
            sending = uiState.composerSending,
            onValueChange = onReplyTextChange,
            onDismiss = onReplyDismiss,
            onSend = onReplySend,
        )
    }
}

@Composable
private fun TopicHeader(
    topic: Topic?,
    onAvatarClick: (String) -> Unit,
    onNodeClick: (String) -> Unit,
    onHtmlLinkClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val loadingText = stringResource(R.string.topic_loading)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (topic == null) {
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.titleMedium,
                )
                return@Column
            }

            Text(
                text = topic.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = topic.member?.avatarNormalUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            topic.member?.username?.let(onAvatarClick)
                        },
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.member?.username.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.node?.title.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                topic.node?.name?.let(onNodeClick)
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = topic.showCreated(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "${topic.replies ?: 0}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            TopicRichText(
                html = topic.content_rendered,
                onLinkClick = onHtmlLinkClick,
                onImageClick = onImageClick,
            )

            if (topic.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                topic.comments.forEach { comment ->
                    CommentCard(
                        comment = comment,
                        onHtmlLinkClick = onHtmlLinkClick,
                        onImageClick = onImageClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerDefaults.color)
        }
    }
}

@Composable
private fun CommentCard(
    comment: Comment,
    onHtmlLinkClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = comment.createdOriginal,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TopicRichText(
            html = comment.content,
            onLinkClick = onHtmlLinkClick,
            onImageClick = onImageClick,
        )
    }
}

@Composable
private fun ReplyCard(
    reply: Reply,
    rowNum: Int,
    onAvatarClick: (String) -> Unit,
    onReply: () -> Unit,
    onThank: () -> Unit,
    onMore: () -> Unit,
    onHtmlLinkClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val actionIconSize = 18.dp
    val replyText = stringResource(R.string.reply)
    val opBadge = stringResource(R.string.topic_op_badge)
    val rowNumText = stringResource(R.string.topic_floor_format, rowNum)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = reply.member?.avatarNormalUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        reply.member?.username?.let(onAvatarClick)
                    },
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reply.member?.username.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rowNumText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (reply.isLouzu) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = opBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    text = reply.showTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TopicRichText(
            html = reply.content_rendered,
            onLinkClick = onHtmlLinkClick,
            onImageClick = onImageClick,
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onThank, enabled = !reply.isThanked) {
                Icon(
                    imageVector = Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(actionIconSize),
                    tint = if (reply.isThanked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = reply.thanks.toString())
            }
            TextButton(onClick = onReply) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(actionIconSize),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = replyText)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DividerDefaults.color)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopicActionDock(
    favored: Boolean,
    thanked: Boolean,
    onFavor: () -> Unit,
    onThank: () -> Unit,
    onReply: () -> Unit,
    onShare: () -> Unit,
) {
    val favorText = stringResource(R.string.favor)
    val thanksText = stringResource(R.string.thanks)
    val replyText = stringResource(R.string.reply)
    val shareText = stringResource(R.string.menu_share)
    val haptic = LocalHapticFeedback.current
    val performDockHaptic = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val iconButtonWeight = 0.72f
    val replyButtonWeight = 1.84f
    val iconButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    val iconButtonShapes = ToggleButtonDefaults.shapes(
        shape = RoundedCornerShape(16.dp),
        pressedShape = RoundedCornerShape(14.dp),
        checkedShape = RoundedCornerShape(16.dp),
    )
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }
                ToggleButton(
                    checked = favored,
                    onCheckedChange = {
                        performDockHaptic()
                        onFavor()
                    },
                    interactionSource = interactionSource,
                    shapes = iconButtonShapes,
                    contentPadding = iconButtonPadding,
                    modifier = Modifier
                        .weight(iconButtonWeight)
                        .animateWidth(interactionSource),
                ) {
                    Icon(
                        imageVector = if (favored) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = favorText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(favorText) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (favored) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        performDockHaptic()
                        onFavor()
                        menuState.dismiss()
                    },
                )
            },
        )
        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }
                ToggleButton(
                    checked = thanked,
                    onCheckedChange = {
                        if (!thanked) {
                            performDockHaptic()
                            onThank()
                        }
                    },
                    interactionSource = interactionSource,
                    shapes = iconButtonShapes,
                    contentPadding = iconButtonPadding,
                    modifier = Modifier
                        .weight(iconButtonWeight)
                        .animateWidth(interactionSource),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = thanksText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(thanksText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        if (!thanked) {
                            performDockHaptic()
                            onThank()
                        }
                        menuState.dismiss()
                    },
                )
            },
        )
        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        performDockHaptic()
                        onReply()
                    },
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(replyButtonWeight)
                        .animateWidth(interactionSource),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = replyText,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = replyText)
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(replyText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        performDockHaptic()
                        onReply()
                        menuState.dismiss()
                    },
                )
            },
        )
        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        performDockHaptic()
                        onShare()
                    },
                    interactionSource = interactionSource,
                    shapes = iconButtonShapes,
                    contentPadding = iconButtonPadding,
                    modifier = Modifier
                        .weight(iconButtonWeight)
                        .animateWidth(interactionSource),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = shareText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(shareText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        performDockHaptic()
                        onShare()
                        menuState.dismiss()
                    },
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportReasonSheet(
    reasons: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.topic_select_reason),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            reasons.forEach { reason ->
                TextButton(
                    onClick = { onSelect(reason) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    Text(text = reason, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplyActionSheet(
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onThank: () -> Unit,
    onHide: () -> Unit,
    onReport: () -> Unit,
    onCopy: () -> Unit,
    onShowUserReplies: () -> Unit,
    onShowConversation: () -> Unit,
) {
    val replyText = stringResource(R.string.reply)
    val thanksText = stringResource(R.string.thanks)
    val hideText = stringResource(R.string.hide_reply)
    val reportText = stringResource(R.string.report_abuse)
    val copyText = stringResource(R.string.copy_content)
    val allRepliesText = stringResource(R.string.show_user_all_reply)
    val conversationText = stringResource(R.string.show_conversation)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            listOf(
                replyText to onReply,
                thanksText to onThank,
                hideText to onHide,
                reportText to onReport,
                copyText to onCopy,
                allRepliesText to onShowUserReplies,
                conversationText to onShowConversation,
            ).forEach { (label, action) ->
                TextButton(
                    onClick = action,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = label, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserRepliesBottomSheet(
    title: String,
    replies: List<Reply>,
    onDismiss: () -> Unit,
    onAvatarClick: (String) -> Unit,
    onReply: (Reply, Int) -> Unit,
    onThank: (Reply) -> Unit,
    onMore: (Reply) -> Unit,
    onHtmlLinkClick: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
        ) {
            itemsIndexed(
                items = replies,
            ) { index, item ->
                ReplyCard(
                    reply = item,
                    rowNum = item.getRowNum(index + 1),
                    onAvatarClick = onAvatarClick,
                    onReply = { onReply(item, item.getRowNum(index + 1)) },
                    onThank = { onThank(item) },
                    onMore = { onMore(item) },
                    onHtmlLinkClick = onHtmlLinkClick,
                    onImageClick = onImageClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplyComposerSheet(
    value: String,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    val replyText = stringResource(R.string.reply)
    val cancelText = stringResource(R.string.cancel)
    val sendText = stringResource(R.string.send)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                value = value,
                onValueChange = onValueChange,
                enabled = !sending,
                label = {
                    Text(text = replyText)
                },
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !sending) {
                    Text(cancelText)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSend, enabled = !sending && value.isNotBlank()) {
                    if (sending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(sendText)
                }
            }
        }
    }
}

private fun buildConversationReplies(allReplies: List<Reply>, current: Reply): List<Reply> {
    val currentUser = current.member?.username.orEmpty()
    if (currentUser.isBlank()) {
        return listOf(current)
    }
    if (!current.content_rendered.contains("v2ex.com/member/")) {
        return listOf(current)
    }
    val inWordUser = Regex("(?<=v2ex\\.com/member/)\\w+")
        .find(current.content_rendered)
        ?.value
        .orEmpty()
    if (inWordUser.isBlank()) {
        return listOf(current)
    }

    return allReplies.filter { item ->
        val username = item.member?.username
        item.id == current.id ||
            (username == currentUser && hasRelate(inWordUser, item)) ||
            (username == inWordUser && hasRelate(currentUser, item)) ||
            (username == inWordUser && item.getRowNum() < current.getRowNum() && !hasOnlyOther(currentUser, item))
    }
}

private fun hasOnlyOther(name: String, item: Reply): Boolean {
    val findOther = "v2ex\\.com/member/(?!$name)".toRegex().containsMatchIn(item.content_rendered)
    val findMe = "v2ex\\.com/member/$name".toRegex().containsMatchIn(item.content_rendered)
    return findOther && !findMe
}

private fun hasRelate(name: String, item: Reply): Boolean {
    return "v2ex\\.com/member/$name".toRegex().containsMatchIn(item.content_rendered)
}

@Preview(showBackground = true)
@Composable
private fun TopicDetailScreenPreview() {
    val sampleTopic = Topic(
        id = "1",
        title = "Compose 重构 Topic 详情页",
        content_rendered = "<p>这是一段正文，带有 <a href=\"https://www.v2ex.com/member/test\">链接</a>。</p>",
        replies = 23,
        member = Member(username = "fdx", avatar_normal = "//cdn.v2ex.co/avatar/test.png"),
        node = Node(name = "compose", title = "Jetpack Compose"),
        createdOriginal = "2 小时前",
    ).apply {
        comments = mutableListOf(
            Comment().apply {
                title = "附言"
                createdOriginal = "1 小时前"
                content = "<p>附言内容</p>"
            },
        )
    }

    val sampleReplies = listOf(
        Reply(
            id = "r1",
            content = "hello",
            content_rendered = "<p>第一条回复</p>",
            thanks = 3,
            member = Member(username = "alice", avatar_normal = "//cdn.v2ex.co/avatar/alice.png"),
            showTime = "20 分钟前",
            isLouzu = false,
        ),
        Reply(
            id = "r2",
            content = "world",
            content_rendered = "<p>@alice 这里是第二条回复</p>",
            thanks = 1,
            member = Member(username = "fdx", avatar_normal = "//cdn.v2ex.co/avatar/fdx.png"),
            showTime = "10 分钟前",
            isLouzu = true,
        ),
    )

    V2exTheme {
        TopicDetailScreen(
            uiState = TopicDetailUiState(
                topicId = "1",
                topic = sampleTopic,
                replies = sampleReplies,
                isLoading = false,
                isRefreshing = false,
            ),
            listState = rememberLazyListState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRefresh = {},
            onLoadMore = {},
            onFavor = {},
            onThankTopic = {},
            onIgnoreTopic = {},
            onShare = {},
            onOpenInBrowser = {},
            onReply = {},
            onReplyTo = { _, _ -> },
            onReplyTextChange = {},
            onReplyDismiss = {},
            onReplySend = {},
            onReportTopic = {},
            onReportReply = { _, _, _ -> },
            onThankReply = {},
            onHideReply = {},
            onAvatarClick = {},
            onNodeClick = {},
            onHtmlLinkClick = {},
            onImageClick = { _, _ -> },
        )
    }
}
