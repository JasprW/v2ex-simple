package im.fdx.v2ex.ui.member.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.member.MemberUiState
import im.fdx.v2ex.utils.TimeUtil

@Composable
fun MemberRoute(
    uiState: MemberUiState,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenGithub: () -> Unit,
    onOpenTwitter: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenBitcoin: () -> Unit,
    pagerContent: @Composable (selectedTab: Int, onTabChanged: (Int) -> Unit) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val tabs = listOf(
        tabLabel(stringResource(id = R.string.member_tab_topics), uiState.topicCount),
        tabLabel(stringResource(id = R.string.member_tab_replies), uiState.replyCount),
    )

    MemberScreen(
        uiModel = uiState.toUiModel(context),
        tabs = tabs,
        selectedTab = selectedTab,
        isFollowed = uiState.isFollowed,
        isBlocked = uiState.isBlocked,
        showRelationActions = !uiState.isMe,
        onBack = onBack,
        onSelectTab = { selectedTab = it },
        onToggleFollow = onToggleFollow,
        onToggleBlock = onToggleBlock,
        onReport = onReport,
        onOpenLocation = onOpenLocation,
        onOpenGithub = onOpenGithub,
        onOpenTwitter = onOpenTwitter,
        onOpenWebsite = onOpenWebsite,
        onOpenBitcoin = onOpenBitcoin,
        pagerContent = {
            pagerContent(selectedTab) { page ->
                selectedTab = page
            }
        },
    )
}

private fun tabLabel(title: String, count: String?): String {
    return if (count.isNullOrBlank()) title else "$title ($count)"
}

private fun MemberUiState.toUiModel(context: android.content.Context): MemberProfileUiModel? {
    val user = member ?: return null
    val joinedDate = TimeUtil.getAbsoluteTime(user.created)
    val joinedDescription = context.getString(
        R.string.member_joined_description,
        joinedDate,
        user.id,
    )
    return MemberProfileUiModel(
        username = user.username,
        avatarUrl = user.avatarLargeUrl,
        tagline = user.tagline,
        intro = user.bio,
        joinedDescription = joinedDescription,
        isOnline = isOnline,
        location = user.location,
        github = user.github,
        twitter = user.twitter,
        website = user.website,
        btc = user.btc,
    )
}
