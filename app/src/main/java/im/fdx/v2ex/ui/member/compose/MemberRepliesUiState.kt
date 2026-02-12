package im.fdx.v2ex.ui.member.compose

import im.fdx.v2ex.ui.member.MemberReplyModel

data class MemberRepliesUiState(
    val username: String = "",
    val items: List<MemberReplyModel> = emptyList(),
    val currentPage: Int = 1,
    val totalPage: Int = 1,
    val totalReplies: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
)
