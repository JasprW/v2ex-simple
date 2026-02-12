package im.fdx.v2ex.ui.member.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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

    MemberScreen(
        uiModel = uiState.toUiModel(),
        tabs = uiState.tabs,
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

private fun MemberUiState.toUiModel(): MemberProfileUiModel? {
    val user = member ?: return null
    return MemberProfileUiModel(
        username = user.username,
        avatarUrl = user.avatarLargeUrl,
        tagline = user.tagline,
        intro = user.bio,
        joinedDescription = "加入于${TimeUtil.getAbsoluteTime(user.created)},第 ${user.id} 号会员",
        isOnline = isOnline,
        location = user.location,
        github = user.github,
        twitter = user.twitter,
        website = user.website,
        btc = user.btc,
    )
}
