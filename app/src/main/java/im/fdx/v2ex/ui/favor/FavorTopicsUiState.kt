package im.fdx.v2ex.ui.favor

import im.fdx.v2ex.ui.main.Topic

data class FavorTopicsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<Topic> = emptyList(),
    val currentPage: Int = 1,
    val totalPage: Int = 1,
    val isEmpty: Boolean = false,
)
