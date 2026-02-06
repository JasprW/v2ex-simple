package im.fdx.v2ex.ui.topic

import androidx.annotation.StringRes
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import im.fdx.v2ex.R
import im.fdx.v2ex.myApp
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.pref
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.topic.data.TopicDetailAuthException
import im.fdx.v2ex.ui.topic.data.TopicDetailRepository
import im.fdx.v2ex.ui.topic.data.TopicDetailRepositoryImpl
import im.fdx.v2ex.utils.TimeUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val PREF_ADD_ROW = "pref_add_row"

data class TopicDetailUiState(
    val topicId: String,
    val topic: Topic? = null,
    val replies: List<Reply> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val once: String? = null,
    val isFavored: Boolean = false,
    val isThanked: Boolean = false,
    val isIgnored: Boolean = false,
    val isFavorUpdating: Boolean = false,
    val isThankUpdating: Boolean = false,
    val draft: String = "",
    val composerText: String = "",
    val composerVisible: Boolean = false,
    val composerSending: Boolean = false,
) {
    val canLoadMore: Boolean = !isLoading && !isRefreshing && !isLoadingMore && currentPage in 1 until totalPages
}

sealed interface TopicDetailEffect {
    data class ShowMessage(val message: String) : TopicDetailEffect
    data object RequestLogin : TopicDetailEffect
    data class ShareText(val text: String) : TopicDetailEffect
    data class OpenBrowser(val url: String, val preferChrome: Boolean) : TopicDetailEffect
    data class OpenMember(val username: String) : TopicDetailEffect
    data class OpenNode(val nodeName: String) : TopicDetailEffect
    data class OpenTopic(val topicId: String) : TopicDetailEffect
    data class OpenPhotos(val photos: List<String>, val position: Int) : TopicDetailEffect
    data class TopicIgnored(val topicId: String, val ignored: Boolean) : TopicDetailEffect
    data object ScrollToBottom : TopicDetailEffect
}

class TopicDetailViewModel(
    private val topicId: String,
    initialTopic: Topic?,
    private val repository: TopicDetailRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TopicDetailUiState(
            topicId = topicId,
            topic = initialTopic,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<TopicDetailEffect>()
    val effects: SharedFlow<TopicDetailEffect> = _effects.asSharedFlow()

    private val shouldAddRow by lazy { pref.getBoolean(PREF_ADD_ROW, false) }

    init {
        viewModelScope.launch {
            val draft = repository.loadDraft(topicId)
            _uiState.update { it.copy(draft = draft) }
        }
        refresh()
    }

    fun refresh(scrollToBottom: Boolean = false) {
        if (_uiState.value.isRefreshing) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, isLoading = it.currentPage == 0) }
            try {
                val pageData = repository.loadTopicPage(topicId = topicId, page = 1)
                val topic = pageData.topic ?: _uiState.value.topic
                val replies = enhanceReplies(topic, pageData.replies)
                _uiState.update {
                    it.copy(
                        topic = topic,
                        replies = replies,
                        isRefreshing = false,
                        isLoading = false,
                        currentPage = pageData.currentPage,
                        totalPages = pageData.totalPages,
                        once = pageData.once,
                        isFavored = pageData.isFavored,
                        isThanked = pageData.isThanked,
                        isIgnored = pageData.isIgnored,
                    )
                }
                if (scrollToBottom) {
                    _effects.emit(TopicDetailEffect.ScrollToBottom)
                }
            } catch (_: TopicDetailAuthException) {
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
                _effects.emit(TopicDetailEffect.RequestLogin)
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_error_open)))
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.canLoadMore) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val nextPage = state.currentPage + 1
                val pageData = repository.loadTopicPage(topicId = topicId, page = nextPage)
                val merged = (state.replies + pageData.replies)
                    .distinctBy { reply -> reply.id }
                _uiState.update {
                    it.copy(
                        replies = enhanceReplies(it.topic, merged),
                        isLoadingMore = false,
                        currentPage = pageData.currentPage,
                        totalPages = pageData.totalPages,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_error_load_more_replies)))
            }
        }
    }

    fun onFavorClick() {
        val state = _uiState.value
        if (!ensureLogin()) {
            return
        }
        val once = state.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_error_action_retry_refresh)
            return
        }
        if (state.isFavorUpdating) {
            return
        }
        val previous = state.isFavored
        _uiState.update { it.copy(isFavored = !previous, isFavorUpdating = true) }
        viewModelScope.launch {
            try {
                repository.toggleFavorite(topicId = topicId, once = once, favored = previous)
                _uiState.update { it.copy(isFavorUpdating = false) }
                _effects.emit(
                    TopicDetailEffect.ShowMessage(
                        if (previous) {
                            str(R.string.topic_unfavor_success)
                        } else {
                            str(R.string.topic_favor_success)
                        },
                    ),
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isFavored = previous, isFavorUpdating = false) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_favor_failed)))
            }
        }
    }

    fun onThankTopicClick() {
        val state = _uiState.value
        if (!ensureLogin()) {
            return
        }
        if (state.isThanked || state.isThankUpdating) {
            return
        }
        val once = state.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_error_action_retry_refresh)
            return
        }

        _uiState.update { it.copy(isThanked = true, isThankUpdating = true) }
        viewModelScope.launch {
            try {
                repository.thankTopic(topicId, once)
                _uiState.update { it.copy(isThankUpdating = false) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_thank_success)))
            } catch (e: Exception) {
                _uiState.update { it.copy(isThanked = false, isThankUpdating = false) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_thank_failed)))
            }
        }
    }

    fun onIgnoreTopicClick() {
        val state = _uiState.value
        if (!ensureLogin()) {
            return
        }
        val once = state.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_error_action_retry_refresh)
            return
        }
        viewModelScope.launch {
            try {
                repository.ignoreTopic(topicId, once, ignored = state.isIgnored)
                _effects.emit(TopicDetailEffect.TopicIgnored(topicId, ignored = !state.isIgnored))
            } catch (e: Exception) {
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_block_failed)))
            }
        }
    }

    fun onShareTopicClick() {
        val topic = _uiState.value.topic ?: return
        viewModelScope.launch {
            val topicUrl = "${NetManager.HTTPS_V2EX_BASE}/t/${topic.id}"
            _effects.emit(
                TopicDetailEffect.ShareText(
                    str(R.string.topic_share_text, topic.title, topicUrl),
                ),
            )
        }
    }

    fun onOpenInBrowserClick() {
        val topic = _uiState.value.topic ?: return
        viewModelScope.launch {
            _effects.emit(
                TopicDetailEffect.OpenBrowser(
                    url = "${NetManager.HTTPS_V2EX_BASE}/t/${topic.id}",
                    preferChrome = true,
                ),
            )
        }
    }

    fun onReplyClick(prefill: String = "") {
        if (!ensureLogin()) {
            return
        }
        val once = _uiState.value.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_reply_send_failed_refresh)
            return
        }
        _uiState.update {
            val initial = buildString {
                append(it.draft)
                if (it.draft.isNotEmpty() && !it.draft.endsWith(" ") && prefill.isNotEmpty()) {
                    append(" ")
                }
                append(prefill)
            }
            it.copy(
                composerVisible = true,
                composerText = initial,
                composerSending = false,
            )
        }
    }

    fun onReplyTo(reply: Reply, rowNum: Int) {
        val username = reply.member?.username.orEmpty()
        if (username.isBlank()) {
            return
        }
        val prefix = if (shouldAddRow) "@$username #$rowNum " else "@$username "
        onReplyClick(prefix)
    }

    fun onComposerTextChange(value: String) {
        _uiState.update { it.copy(composerText = value) }
    }

    fun onComposerDismiss() {
        _uiState.update {
            it.copy(
                composerVisible = false,
                composerSending = false,
                draft = it.composerText,
            )
        }
    }

    fun onComposerSend() {
        val state = _uiState.value
        if (!ensureLogin()) {
            return
        }
        val once = state.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_reply_send_failed_refresh)
            return
        }
        val content = state.composerText.trim()
        if (content.isBlank() || state.composerSending) {
            return
        }
        _uiState.update { it.copy(composerSending = true) }
        viewModelScope.launch {
            try {
                repository.submitReply(topicId = topicId, once = once, content = content)
                _uiState.update {
                    it.copy(
                        composerVisible = false,
                        composerSending = false,
                        composerText = "",
                        draft = "",
                    )
                }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_reply_post_success)))
                refresh(scrollToBottom = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(composerSending = false) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_reply_post_failed)))
            }
        }
    }

    fun onThankReply(reply: Reply) {
        if (!ensureLogin()) {
            return
        }
        val once = _uiState.value.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_refresh_retry)
            return
        }
        if (reply.isThanked) {
            return
        }
        val previousReplies = _uiState.value.replies
        val optimisticReplies = previousReplies.map {
            if (it.id == reply.id) {
                it.copy(isThanked = true, thanks = it.thanks + 1)
            } else {
                it
            }
        }
        _uiState.update { it.copy(replies = optimisticReplies) }

        viewModelScope.launch {
            try {
                val newOnce = repository.thankReply(reply.id, once)
                if (!newOnce.isNullOrBlank()) {
                    _uiState.update { it.copy(once = newOnce) }
                }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_thank_success)))
            } catch (e: Exception) {
                _uiState.update { it.copy(replies = previousReplies) }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_thank_failed)))
            }
        }
    }

    fun onHideReply(reply: Reply) {
        if (!ensureLogin()) {
            return
        }
        val once = _uiState.value.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_refresh_retry)
            return
        }
        viewModelScope.launch {
            try {
                repository.hideReply(reply.id, once)
                _uiState.update {
                    it.copy(replies = it.replies.filterNot { item -> item.id == reply.id })
                }
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_hide_success)))
            } catch (e: Exception) {
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_hide_failed)))
            }
        }
    }

    fun onReportTopic(reason: String) {
        postReport(str(R.string.topic_report_topic_template, reason))
    }

    fun onReportReply(reply: Reply, rowNum: Int, reason: String) {
        postReport(str(R.string.topic_report_reply_template, rowNum, reason))
    }

    fun onAvatarClick(username: String) {
        if (username.isBlank()) {
            return
        }
        viewModelScope.launch {
            _effects.emit(TopicDetailEffect.OpenMember(username))
        }
    }

    fun onNodeClick(nodeName: String) {
        if (nodeName.isBlank()) {
            return
        }
        viewModelScope.launch {
            _effects.emit(TopicDetailEffect.OpenNode(nodeName))
        }
    }

    fun onHtmlLinkClick(url: String) {
        viewModelScope.launch {
            when {
                url.contains("v2ex.com/member/") -> {
                    val username = url.substringAfterLast("/")
                    _effects.emit(TopicDetailEffect.OpenMember(username))
                }

                url.contains("v2ex.com/go/") -> {
                    val nodeName = url.substringAfterLast("/")
                    _effects.emit(TopicDetailEffect.OpenNode(nodeName))
                }

                url.contains("v2ex.com/t/") -> {
                    val parsedTopicId = Regex("(?<=/t/)\\d+").find(url)?.value
                    if (!parsedTopicId.isNullOrBlank()) {
                        _effects.emit(TopicDetailEffect.OpenTopic(parsedTopicId))
                    } else {
                        _effects.emit(TopicDetailEffect.OpenBrowser(url, preferChrome = false))
                    }
                }

                else -> _effects.emit(TopicDetailEffect.OpenBrowser(url, preferChrome = false))
            }
        }
    }

    fun onRichImageClick(allImages: List<String>, index: Int) {
        if (allImages.isEmpty()) {
            return
        }
        viewModelScope.launch {
            _effects.emit(TopicDetailEffect.OpenPhotos(allImages, index))
        }
    }

    fun persistDraft() {
        val state = _uiState.value
        val draft = if (state.composerVisible) state.composerText else state.draft
        viewModelScope.launch {
            repository.saveDraft(topicId, draft)
        }
    }

    override fun onCleared() {
        val state = _uiState.value
        val draft = if (state.composerVisible) state.composerText else state.draft
        runBlocking {
            repository.saveDraft(topicId, draft)
        }
        super.onCleared()
    }

    private fun postReport(content: String) {
        val state = _uiState.value
        if (!ensureLogin()) {
            return
        }
        val once = state.once
        if (once.isNullOrBlank()) {
            emitMessage(R.string.topic_error_action_retry_refresh)
            return
        }
        viewModelScope.launch {
            try {
                repository.submitReply(topicId = topicId, once = once, content = content)
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_report_submitted)))
                refresh()
            } catch (e: Exception) {
                _effects.emit(TopicDetailEffect.ShowMessage(str(R.string.topic_error_retry)))
            }
        }
    }

    private fun enhanceReplies(topic: Topic?, replies: List<Reply>): List<Reply> {
        val owner = topic?.member?.username
        return replies.mapIndexed { index, reply ->
            reply.copy(
                isLouzu = reply.member?.username == owner,
                showTime = TimeUtil.getReplyTime(reply.created).ifBlank { reply.createdOriginal },
            ).apply {
                if (getRowNum() <= 0) {
                    setRowNum(index + 1)
                }
            }
        }
    }

    private fun ensureLogin(): Boolean {
        if (myApp.isLogin) {
            return true
        }
        viewModelScope.launch {
            _effects.emit(TopicDetailEffect.RequestLogin)
        }
        return false
    }

    private fun emitMessage(content: String) {
        viewModelScope.launch {
            _effects.emit(TopicDetailEffect.ShowMessage(content))
        }
    }

    private fun emitMessage(@StringRes resId: Int, vararg formatArgs: Any) {
        emitMessage(str(resId, *formatArgs))
    }

    private fun str(@StringRes resId: Int, vararg formatArgs: Any): String {
        return myApp.getString(resId, *formatArgs)
    }

    companion object {
        fun factory(
            topicId: String,
            initialTopic: Topic?,
            repository: TopicDetailRepository = TopicDetailRepositoryImpl(),
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TopicDetailViewModel(topicId, initialTopic, repository) as T
                }
            }
        }
    }
}
