package im.fdx.v2ex.ui.member

data class MemberUiState(
    val username: String = "",
    val isLoading: Boolean = true,
    val member: Member? = null,
    val isMe: Boolean = false,
    val isBlocked: Boolean = false,
    val isFollowed: Boolean = false,
    val isOnline: Boolean = false,
    val blockTokenPath: String? = null,
    val followTokenPath: String? = null,
    val topicCount: String? = null,
    val replyCount: String? = null,
)
