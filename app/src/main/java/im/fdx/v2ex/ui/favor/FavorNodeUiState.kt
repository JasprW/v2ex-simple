package im.fdx.v2ex.ui.favor

import im.fdx.v2ex.ui.node.Node

data class FavorNodeUiState(
    val isLoading: Boolean = false,
    val nodes: List<Node> = emptyList(),
    val isEmpty: Boolean = false,
)
