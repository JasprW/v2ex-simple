package im.fdx.v2ex.ui.node

data class NodeSection(
    val category: String,
    val nodes: List<Node>,
)

data class AllNodesUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val sections: List<NodeSection> = emptyList(),
    val isEmpty: Boolean = false,
)
