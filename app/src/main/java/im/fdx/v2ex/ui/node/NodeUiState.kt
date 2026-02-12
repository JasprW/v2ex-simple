package im.fdx.v2ex.ui.node

import im.fdx.v2ex.ui.main.Topic

data class NodeUiState(
    val nodeName: String = "",
    val node: Node? = null,
    val topics: List<Topic> = emptyList(),
    val pageNum: Int = 1,
    val currentPage: Int = 1,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isFollowed: Boolean = false,
    val followToken: String? = null,
)
