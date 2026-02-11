package im.fdx.v2ex.ui.member

import im.fdx.v2ex.ui.main.Topic

/**
 * Member 页面的 UI 状态
 */
sealed interface MemberUiState {
    data object Loading : MemberUiState
    data class Success(
        val member: Member,
        val isFollowed: Boolean,
        val isBlocked: Boolean,
        val isOnline: Boolean,
        val isMe: Boolean,
    ) : MemberUiState
    data class Error(val message: String) : MemberUiState
}

/**
 * 标签页类型
 */
enum class MemberTab(val title: String) {
    TOPICS("主题"),
    REPLIES("回复")
}

/**
 * Member 回复页面的 UI 状态
 */
sealed interface MemberRepliesUiState {
    data object Loading : MemberRepliesUiState
    data class Success(
        val replies: List<MemberReplyModel>,
        val currentPage: Int,
        val totalPages: Int,
    ) : MemberRepliesUiState
    data class Error(val message: String) : MemberRepliesUiState
}

/**
 * Member 主题页面的 UI 状态
 */
sealed interface MemberTopicsUiState {
    data object Loading : MemberTopicsUiState
    data class Success(
        val topics: List<Topic>,
        val currentPage: Int,
        val totalPages: Int,
    ) : MemberTopicsUiState
    data class Error(val message: String) : MemberTopicsUiState
}

/**
 * 社交链接信息
 */
data class SocialLinks(
    val location: String? = null,
    val github: String? = null,
    val twitter: String? = null,
    val website: String? = null,
    val btc: String? = null,
)
