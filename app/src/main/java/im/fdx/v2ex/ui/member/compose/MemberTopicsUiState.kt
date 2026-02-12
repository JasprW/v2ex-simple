package im.fdx.v2ex.ui.member.compose

import im.fdx.v2ex.ui.main.Topic

data class MemberTopicsUiState(
    val username: String = "",
    val avatar: String = "",
    val items: List<Topic> = emptyList(),
    val currentPage: Int = 1,
    val totalPage: Int = 1,
    val totalTopics: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hiddenMessage: String? = null,
    val errorMessage: String? = null,
)
