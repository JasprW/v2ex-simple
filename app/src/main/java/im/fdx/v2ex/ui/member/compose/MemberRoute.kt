package im.fdx.v2ex.ui.member.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.fdx.v2ex.ui.member.MemberViewModel

/**
 * Member 页面的路由组件
 * 连接 ViewModel 和 UI
 */
@Composable
fun MemberRoute(
    onNavigateBack: () -> Unit,
    onTopicClick: (String) -> Unit,
    onReportClick: (String, String) -> Unit,
    onShowLoginHint: () -> Unit,
    viewModel: MemberViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val repliesState by viewModel.repliesState.collectAsStateWithLifecycle()
    val topicsState by viewModel.topicsState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    MemberScreen(
        uiState = uiState,
        repliesState = repliesState,
        topicsState = topicsState,
        selectedTab = selectedTab,
        onNavigateBack = onNavigateBack,
        onTopicClick = onTopicClick,
        onReportClick = onReportClick,
        onShowLoginHint = onShowLoginHint,
        onToggleFollow = { message ->
            // 显示 Toast 或 Snackbar
        },
        onToggleBlock = { message ->
            // 显示 Toast 或 Snackbar
        },
        onSelectTab = viewModel::selectTab,
        onRefreshReplies = viewModel::refreshReplies,
        onLoadMoreReplies = viewModel::loadMoreReplies,
        onRefreshTopics = viewModel::refreshTopics,
        onLoadMoreTopics = viewModel::loadMoreTopics,
    )
}
