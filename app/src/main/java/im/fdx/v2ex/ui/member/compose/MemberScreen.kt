package im.fdx.v2ex.ui.member.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.member.Member
import im.fdx.v2ex.ui.member.MemberRepliesUiState
import im.fdx.v2ex.ui.member.MemberTab
import im.fdx.v2ex.ui.member.MemberTopicsUiState
import im.fdx.v2ex.ui.member.MemberUiState

/**
 * Member 页面主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemberScreen(
    uiState: MemberUiState,
    repliesState: MemberRepliesUiState,
    topicsState: MemberTopicsUiState,
    selectedTab: MemberTab,
    onNavigateBack: () -> Unit,
    onTopicClick: (String) -> Unit,
    onReportClick: (String, String) -> Unit,
    onShowLoginHint: () -> Unit,
    onToggleFollow: (String) -> Unit,
    onToggleBlock: (String) -> Unit,
    onSelectTab: (MemberTab) -> Unit,
    onRefreshReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit,
    onRefreshTopics: () -> Unit,
    onLoadMoreTopics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { MemberTab.entries.size })
    var showMenu by remember { mutableStateOf(false) }

    // 同步 Tab 和 Pager
    LaunchedEffect(selectedTab) {
        val pageIndex = MemberTab.entries.indexOf(selectedTab)
        if (pagerState.currentPage != pageIndex) {
            pagerState.animateScrollToPage(pageIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val tab = MemberTab.entries[pagerState.currentPage]
        if (selectedTab != tab) {
            onSelectTab(tab)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState) {
                            is MemberUiState.Success -> uiState.member.username
                            else -> "用户资料"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState is MemberUiState.Success && !uiState.isMe) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // 关注/取消关注
                            DropdownMenuItem(
                                text = {
                                    Text(if (uiState.isFollowed) "取消关注" else "关注")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (uiState.isFollowed) {
                                            Icons.Default.Favorite
                                        } else {
                                            Icons.Default.FavoriteBorder
                                        },
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleFollow(
                                        if (uiState.isFollowed) "取消关注成功" else "关注成功"
                                    )
                                }
                            )

                            // 屏蔽/取消屏蔽
                            DropdownMenuItem(
                                text = {
                                    Text(if (uiState.isBlocked) "取消屏蔽" else "屏蔽")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleBlock(
                                        if (uiState.isBlocked) {
                                            "你已取消屏蔽该用户"
                                        } else {
                                            "屏蔽成功，你将无法看到该用户的帖子和评论"
                                        }
                                    )
                                }
                            )

                            // 举报
                            DropdownMenuItem(
                                text = { Text("举报") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Report,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onReportClick(
                                        uiState.member.username,
                                        "https://www.v2ex.com/member/${uiState.member.username}"
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 用户资料头部
            MemberProfileHeader(
                uiState = uiState,
                modifier = Modifier
            )

            // Tab 栏
            PrimaryTabRow(
                selectedTabIndex = MemberTab.entries.indexOf(selectedTab)
            ) {
                MemberTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            val count = when (tab) {
                                MemberTab.TOPICS -> {
                                    (topicsState as? MemberTopicsUiState.Success)?.topics?.size?.toString() ?: ""
                                }
                                MemberTab.REPLIES -> {
                                    (repliesState as? MemberRepliesUiState.Success)?.replies?.size?.toString() ?: ""
                                }
                            }
                            Text("${tab.title} ${if (count.isNotEmpty()) "($count)" else ""}")
                        }
                    )
                }
            }

            // 内容区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (MemberTab.entries[page]) {
                    MemberTab.TOPICS -> {
                        MemberTopicsContent(
                            state = topicsState,
                            onTopicClick = onTopicClick,
                            onRefresh = onRefreshTopics,
                            onLoadMore = onLoadMoreTopics
                        )
                    }
                    MemberTab.REPLIES -> {
                        MemberRepliesContent(
                            state = repliesState,
                            onTopicClick = onTopicClick,
                            onRefresh = onRefreshReplies,
                            onLoadMore = onLoadMoreReplies
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemberScreenPreview() {
    V2exTheme {
        MemberScreen(
            uiState = MemberUiState.Success(
                member = Member(
                    id = "12345",
                    username = "testuser",
                    tagline = "这是一个测试用户",
                    bio = "这里是个人简介",
                    avatar_normal = "",
                    created = "1234567890"
                ),
                isFollowed = false,
                isBlocked = false,
                isOnline = true,
                isMe = false
            ),
            repliesState = MemberRepliesUiState.Success(
                replies = emptyList(),
                currentPage = 1,
                totalPages = 1
            ),
            topicsState = MemberTopicsUiState.Success(
                topics = emptyList(),
                currentPage = 1,
                totalPages = 1
            ),
            selectedTab = MemberTab.TOPICS,
            onNavigateBack = {},
            onTopicClick = {},
            onReportClick = { _, _ -> },
            onShowLoginHint = {},
            onToggleFollow = {},
            onToggleBlock = {},
            onSelectTab = {},
            onRefreshReplies = {},
            onLoadMoreReplies = {},
            onRefreshTopics = {},
            onLoadMoreTopics = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun MemberScreenLoadingPreview() {
    V2exTheme {
        MemberScreen(
            uiState = MemberUiState.Loading,
            repliesState = MemberRepliesUiState.Loading,
            topicsState = MemberTopicsUiState.Loading,
            selectedTab = MemberTab.TOPICS,
            onNavigateBack = {},
            onTopicClick = {},
            onReportClick = { _, _ -> },
            onShowLoginHint = {},
            onToggleFollow = {},
            onToggleBlock = {},
            onSelectTab = {},
            onRefreshReplies = {},
            onLoadMoreReplies = {},
            onRefreshTopics = {},
            onLoadMoreTopics = {}
        )
    }
}
