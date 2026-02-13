package im.fdx.v2ex.ui.main

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.R
import im.fdx.v2ex.pref
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.node.AllNodesActivity
import im.fdx.v2ex.ui.node.Node
import im.fdx.v2ex.utils.Keys
import java.util.Calendar
import kotlinx.coroutines.delay
import org.json.JSONArray

class SearchActivity : BaseActivity() {

    private lateinit var fra: TopicsFragment
    private var query: SearchOption = SearchOption("")

    private val chooseNodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val nodeInfo = result.data?.getParcelableExtra<Node>(Keys.KEY_NODE) ?: return@registerForActivityResult
            query = query.copy(node = nodeInfo.name)
            selectedNode = nodeInfo
        }
    }

    private var selectedNode by mutableStateOf<Node?>(null)
    private var isSearching by mutableStateOf(false)
    private var searchHistory by mutableStateOf(emptyList<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdgeWindow()

        val shouldShowSearchTip = pref.getBoolean(Keys.KEY_WARN_SEARCH_API, true)
        searchHistory = loadSearchHistory()

        fra = TopicsFragment().apply {
            arguments = bundleOf("search" to true)
            onLoadingChanged = { isSearching = it }
        }

        setContent {
            V2exTheme {
                SearchScreen(
                    initialQuery = query,
                    selectedNode = selectedNode,
                    isRefreshing = isSearching,
                    shouldShowSearchTip = shouldShowSearchTip,
                    searchHistory = searchHistory,
                    onBack = { finish() },
                    onQuerySubmit = { q ->
                        query = q
                        addSearchHistory(q.q)
                        fra.startQuery(query)
                    },
                    onRefresh = {
                        fra.startQuery(query)
                    },
                    onChooseNode = {
                        chooseNodeLauncher.launch(android.content.Intent(this, AllNodesActivity::class.java).apply {
                            putExtras(bundleOf(Keys.KEY_TO_CHOOSE_NODE to true))
                        })
                    },
                    onClearNode = {
                        selectedNode = null
                        query = query.copy(node = null)
                    },
                    onSearchTipAcknowledged = {
                        pref.edit().putBoolean(Keys.KEY_WARN_SEARCH_API, false).apply()
                    },
                    onDeleteHistoryItem = { keyword -> removeSearchHistory(keyword) },
                    onClearHistory = { clearSearchHistory() },
                    onPickGte = { onSet -> showDatePicker(onSet) },
                    onPickLte = { onSet -> showDatePicker(onSet) },
                    fragmentHost = {
                        SearchFragmentHost(fragment = fra)
                    },
                )
            }
        }
    }

    private fun showDatePicker(onSet: (Calendar) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                onSet(date)
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun applyEdgeToEdgeWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val surfaceColor = MaterialColors.getColor(this, R.attr.colorSurface, Color.BLACK)
        val isLight = ColorUtils.calculateLuminance(surfaceColor) > 0.5f
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }

    private fun loadSearchHistory(): List<String> {
        val raw = pref.getString(Keys.KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val keyword = array.optString(index).trim()
                    if (keyword.isNotEmpty()) add(keyword)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistSearchHistory(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        pref.edit().putString(Keys.KEY_SEARCH_HISTORY, array.toString()).apply()
    }

    private fun addSearchHistory(keyword: String) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return
        val updated = listOf(normalized) + searchHistory.filterNot { it.equals(normalized, ignoreCase = true) }
        searchHistory = updated.take(20)
        persistSearchHistory(searchHistory)
    }

    private fun removeSearchHistory(keyword: String) {
        searchHistory = searchHistory.filterNot { it == keyword }
        persistSearchHistory(searchHistory)
    }

    private fun clearSearchHistory() {
        searchHistory = emptyList()
        persistSearchHistory(searchHistory)
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun SearchScreen(
    initialQuery: SearchOption,
    selectedNode: Node?,
    isRefreshing: Boolean,
    shouldShowSearchTip: Boolean,
    searchHistory: List<String>,
    onBack: () -> Unit,
    onQuerySubmit: (SearchOption) -> Unit,
    onRefresh: () -> Unit,
    onChooseNode: () -> Unit,
    onClearNode: () -> Unit,
    onSearchTipAcknowledged: () -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPickGte: ((Calendar) -> Unit) -> Unit,
    onPickLte: ((Calendar) -> Unit) -> Unit,
    fragmentHost: @Composable () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val contentPullOffsetPx = with(density) {
        pullToRefreshState.distanceFraction * PullToRefreshDefaults.IndicatorMaxDistance.toPx()
    }
    val defaultNodeLabel = stringResource(id = R.string.node)
    val searchTipMessage = stringResource(id = R.string.search_api_notice)
    val searchTipAction = stringResource(id = R.string.iknow)
    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
    val textFieldState = rememberTextFieldState(initialQuery.q)
    val sortTitles = stringArrayResource(id = R.array.search_option)
    var sortIndex by remember {
        mutableIntStateOf(
            when {
                initialQuery.sort == SUMUP -> 2
                initialQuery.order == OLD_FIRST -> 1
                else -> 0
            },
        )
    }
    var gteText by remember { mutableStateOf(initialQuery.gte ?: "") }
    var lteText by remember { mutableStateOf(initialQuery.lte ?: "") }
    var nodeText by remember(defaultNodeLabel) { mutableStateOf(selectedNode?.let { "${it.name} | ${it.title}" } ?: defaultNodeLabel) }
    var lastObservedNodeName by remember { mutableStateOf(selectedNode?.name) }
    var hasSubmittedQuery by remember { mutableStateOf(initialQuery.q.isNotBlank()) }
    var lastSubmittedQueryText by remember { mutableStateOf(initialQuery.q.trim()) }
    val currentInputRaw = textFieldState.text.toString()
    val currentInput = currentInputRaw.trim()
    val showHistory = !hasSubmittedQuery || currentInput.isBlank()
    val showDraftMask = hasSubmittedQuery && currentInput.isNotBlank() && currentInput != lastSubmittedQueryText
    val hasNodeFilter = selectedNode != null

    LaunchedEffect(Unit) {
        searchBarState.animateToExpanded()
        delay(120)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(shouldShowSearchTip) {
        if (shouldShowSearchTip) {
            val result = snackbarHostState.showSnackbar(
                message = searchTipMessage,
                actionLabel = searchTipAction,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onSearchTipAcknowledged()
            }
        }
    }

    fun buildQuery(searchText: String = textFieldState.text.toString().trim(), nodeName: String? = selectedNode?.name): SearchOption {
        val sort = when (sortIndex) {
            2 -> SUMUP
            else -> CREATED
        }
        val order = when (sortIndex) {
            1 -> OLD_FIRST
            else -> NEW_FIRST
        }
        return initialQuery.copy(
            q = searchText,
            sort = sort,
            order = order,
            gte = gteText.ifBlank { null },
            lte = lteText.ifBlank { null },
            node = nodeName,
        )
    }

    fun currentQuery(): SearchOption = buildQuery()

    fun submitQuery(query: SearchOption) {
        val normalizedQuery = query.q.trim()
        if (normalizedQuery.isEmpty()) return
        if (query.q != normalizedQuery) {
            textFieldState.edit {
                replace(0, length, normalizedQuery)
            }
        }
        hasSubmittedQuery = true
        lastSubmittedQueryText = normalizedQuery
        onQuerySubmit(query.copy(q = normalizedQuery))
    }

    LaunchedEffect(selectedNode?.name) {
        nodeText = selectedNode?.let { "${it.name} | ${it.title}" } ?: defaultNodeLabel
        val currentNodeName = selectedNode?.name
        val nodeChanged = lastObservedNodeName != currentNodeName
        lastObservedNodeName = currentNodeName
        if (nodeChanged && currentNodeName != null && hasSubmittedQuery) {
            submitQuery(buildQuery(lastSubmittedQueryText))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppBarWithSearch(
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        modifier = Modifier
                            .focusRequester(focusRequester),
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        onSearch = {
                            submitQuery(currentQuery())
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        },
                        placeholder = { Text(stringResource(id = R.string.search_hint)) },
                        leadingIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.topic_action_back),
                                )
                            }
                        },
                        trailingIcon = {
                            if (currentInputRaw.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        textFieldState.edit {
                                            replace(0, length, "")
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(id = R.string.search_history_clear_all),
                                    )
                                }
                            }
                        },
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    AssistChip(
                        onClick = {
                            sortIndex = (sortIndex + 1) % sortTitles.size
                            submitQuery(currentQuery())
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        label = { Text(sortTitles[sortIndex]) },
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            if (gteText.isBlank()) {
                                onPickGte { date ->
                                    gteText = im.fdx.v2ex.utils.TimeUtil.toDisplay(date)
                                    submitQuery(currentQuery())
                                }
                            } else {
                                gteText = ""
                                submitQuery(currentQuery())
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        label = { Text(if (gteText.isBlank()) stringResource(id = R.string.gte) else gteText) },
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            if (lteText.isBlank()) {
                                onPickLte { date ->
                                    lteText = im.fdx.v2ex.utils.TimeUtil.toDisplay(date)
                                    submitQuery(currentQuery())
                                }
                            } else {
                                lteText = ""
                                submitQuery(currentQuery())
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        label = { Text(if (lteText.isBlank()) stringResource(id = R.string.lte) else lteText) },
                    )
                }
                item {
                    InputChip(
                        selected = hasNodeFilter,
                        onClick = onChooseNode,
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        label = { Text(nodeText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = if (hasNodeFilter) {
                            {
                                IconButton(
                                    onClick = {
                                        onClearNode()
                                        if (hasSubmittedQuery) {
                                            submitQuery(buildQuery(lastSubmittedQueryText, nodeName = null))
                                        }
                                    },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(id = R.string.search_clear_node_filter),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                isRefreshing = isRefreshing && currentInput.isNotBlank(),
                onRefresh = {
                    if (currentInput.isNotBlank()) {
                        onRefresh()
                    }
                },
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing && currentInput.isNotBlank(),
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = ComposeColor.Transparent,
                        elevation = 0.dp,
                    )
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = contentPullOffsetPx },
                ) {
                    fragmentHost()
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showHistory,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        ) + expandVertically(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 150),
                        ) + shrinkVertically(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        ),
                    ) {
                        SearchHistoryList(
                            history = searchHistory,
                            onSearchKeyword = { keyword ->
                                textFieldState.edit {
                                    replace(0, length, keyword)
                                }
                                submitQuery(currentQuery().copy(q = keyword))
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            },
                            onDeleteKeyword = onDeleteHistoryItem,
                            onClearAll = onClearHistory,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showDraftMask,
                        enter = fadeIn(animationSpec = tween(durationMillis = 120)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 120)),
                    ) {
                        SearchDraftMask(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchDraftMask(modifier: Modifier = Modifier) {
    val blockTouchInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = blockTouchInteraction,
                indication = null,
                onClick = {},
            ),
    )
}

@Composable
private fun SearchHistoryList(
    history: List<String>,
    onSearchKeyword: (String) -> Unit,
    onDeleteKeyword: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blockTouchInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                    },
                )
            }
            .clickable(
                interactionSource = blockTouchInteraction,
                indication = null,
                onClick = {},
            ),
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.search_history_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearAll) {
                    Text(text = stringResource(id = R.string.search_history_clear_all))
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = R.string.search_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = history, key = { it }) { keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSearchKeyword(keyword) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = keyword,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(onClick = { onDeleteKeyword(keyword) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(id = R.string.search_history_delete),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SearchFragmentHost(fragment: TopicsFragment) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            FragmentContainerView(context).apply {
                id = android.view.View.generateViewId()
            }
        },
        update = { container ->
            val activity = container.context as SearchActivity
            if (activity.supportFragmentManager.findFragmentByTag("search_topics") == null) {
                activity.supportFragmentManager.beginTransaction()
                    .replace(container.id, fragment, "search_topics")
                    .commitAllowingStateLoss()
            }
        },
    )
}
