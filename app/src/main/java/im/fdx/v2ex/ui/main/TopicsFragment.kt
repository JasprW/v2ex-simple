package im.fdx.v2ex.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import im.fdx.v2ex.R
import im.fdx.v2ex.myApp
import im.fdx.v2ex.network.*
import im.fdx.v2ex.network.NetManager.API_HEATED
import im.fdx.v2ex.network.NetManager.HTTPS_V2EX_BASE
import im.fdx.v2ex.network.NetManager.URL_FOLLOWING
import im.fdx.v2ex.network.NetManager.dealError
import im.fdx.v2ex.network.Parser.Source.*
import im.fdx.v2ex.ui.NODE_TYPE
import im.fdx.v2ex.ui.isUsePageNum
import im.fdx.v2ex.ui.main.model.SearchResult
import im.fdx.v2ex.ui.member.Member
import im.fdx.v2ex.ui.member.MemberActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.utils.EndlessOnScrollListener
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.TimeUtil
import im.fdx.v2ex.utils.ViewUtil
import im.fdx.v2ex.utils.extensions.*
import im.fdx.v2ex.view.PageNumberView
import okhttp3.*
import im.fdx.v2ex.utils.extensions.toast
import java.io.IOException
import java.util.*

/**
 * 主题列表页，核心页面。
 * 存在问题:  重构复杂，代码复杂。 代码不优雅，扩展性不够
 */
class TopicsFragment : Fragment() {

    var onLoadingChanged: ((Boolean) -> Unit)? = null
    var onLoadingMoreChanged: ((Boolean) -> Unit)? = null

    private lateinit var mAdapter: TopicsRVAdapter
    private val loadingFooterAdapter = SearchLoadingFooterAdapter()
    private lateinit var mSwipeLayout: SwipeRefreshLayout
    private var mRecyclerView: RecyclerView? = null
    private var fab: FloatingActionButton? = null //有可能为空
    private lateinit var flContainer: FrameLayout
    private var pageNumberView: PageNumberView? = null
    var mRequestURL: String = ""
    private lateinit var mScrollListener: EndlessOnScrollListener
    private var pendingSearchQuery: SearchOption? = null
    var currentMode = FROM_HOME
    var totalPage = 0
    var isEndlessMode = true // 模式无限滚动模式

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.action
            logd("getAction: $action")
            val itemId = intent.getStringExtra(Keys.KEY_TOPIC_ID)
            when (action) {
                Keys.ACTION_HIDE_TOPIC -> {

                    itemId?.let { mAdapter.removeItem(it) }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        LocalBroadcastManager.getInstance(myApp).registerReceiver(receiver, IntentFilter(Keys.ACTION_HIDE_TOPIC))
    }

    override fun onDetach() {
        super.onDetach()
        LocalBroadcastManager.getInstance(myApp).registerReceiver(receiver, IntentFilter(Keys.ACTION_HIDE_TOPIC))
    }


    fun togglePageNum(usePageNum: Boolean) {
        isEndlessMode = !usePageNum
        if (isEndlessMode) {
            mScrollListener?.let { mRecyclerView?.addOnScrollListener(it) }
        } else {
            mScrollListener?.let { mRecyclerView?.removeOnScrollListener(it) }
        }
        pageNumberView?.let {
            it.globalVisible = usePageNum
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_tab_article, container, false)
        mSwipeLayout = layout.findViewById(R.id.swipe_container)
        mSwipeLayout.initTheme()
        mSwipeLayout.setOnRefreshListener { refresh() }
        pageNumberView = layout.findViewById(R.id.pageNumberView)
        mRecyclerView = layout.findViewById(R.id.rv_container)
        val layoutManager = LinearLayoutManager(activity)
        mRecyclerView?.layoutManager = layoutManager

        mScrollListener = object : EndlessOnScrollListener(mRecyclerView!!, layoutManager) {
            override fun onCompleted() {
                activity?.toast(getString(R.string.no_more_data))
            }

            override fun onLoadMore(currentPage: Int) {
                logw("currentPage: $currentPage")
                mScrollListener.loading = true
                if (currentMode != FROM_SEARCH) {
                    mSwipeLayout.isRefreshing = true
                }
                loadMoreTopic(currentPage)
            }
        }

        Log.d("TopicFragment", "onViewCreated:")
        mAdapter = TopicsRVAdapter(this)
        mRecyclerView?.adapter = mAdapter

        flContainer = layout.findViewById(R.id.fl_container)


        val args: Bundle? = arguments
        when {
            args?.getString(Keys.KEY_TAB) == "recent" -> mRequestURL = "$HTTPS_V2EX_BASE/recent"
            args?.getString(Keys.KEY_TAB) == "heated" -> mRequestURL = API_HEATED
            args?.getString(Keys.KEY_TAB) != null -> {
                if (args.getInt(Keys.KEY_TYPE) == NODE_TYPE) {
                    currentMode = FROM_NODE
                    mRequestURL = "$HTTPS_V2EX_BASE/go/${args.getString(Keys.KEY_TAB)}"
                } else {
                    mRequestURL = "$HTTPS_V2EX_BASE/?tab=${args.getString(Keys.KEY_TAB)}"
                }
            }
            args?.getString(Keys.KEY_USERNAME) != null -> {
                currentMode = FROM_MEMBER
                mRequestURL = "$HTTPS_V2EX_BASE/member/${args.getString(Keys.KEY_USERNAME)}/topics"
            }
            args?.getString(Keys.KEY_NODE_NAME) != null -> {
                currentMode = FROM_NODE
                mRequestURL = "$HTTPS_V2EX_BASE/go/${args.getString(Keys.KEY_NODE_NAME)}"
            }
            args?.getBoolean("search", false) == true -> {
                currentMode = FROM_SEARCH
            }
        }

        when (currentMode) {
            FROM_NODE, FROM_SEARCH, FROM_MEMBER -> mRecyclerView?.addOnScrollListener(mScrollListener)
            FROM_HOME -> {
                if (args?.getString(Keys.KEY_TAB) == "recent") {
                    mRecyclerView?.addOnScrollListener(mScrollListener)
                }
            }
            FROM_FAVOR -> Unit

        }
        when(currentMode) {
            FROM_MEMBER -> {
                togglePageNum(isUsePageNum)
            }
            else -> {
                if (args?.getString(Keys.KEY_TAB) == "recent") {
                    isEndlessMode = true
                } else {
                    togglePageNum(false)
                }
            }
        }

        if (currentMode == FROM_SEARCH) {
            mSwipeLayout.isEnabled = false
            mRecyclerView?.adapter = ConcatAdapter(mAdapter, loadingFooterAdapter)
        }

        val topicList: ArrayList<Topic>? = args?.getParcelableArrayList(Keys.KEY_TOPIC_LIST)
        if (currentMode == FROM_SEARCH) {
            flContainer.showNoContent(getString(R.string.please_input_key_to_search))
        } else if (currentMode == FROM_NODE && topicList != null) {
            // 已有数据
            val totalPage = args.getInt(Keys.KEY_PAGE_NUM, 1) ?: 1
//            logd(topicList)
            mScrollListener.totalPage = totalPage
            setUIData(topicList)
        } else {
            flContainer.hideNoContent()
            if (currentMode != FROM_SEARCH) {
                mSwipeLayout.isRefreshing = true
            }
            getTopics(mRequestURL)
        }

        pageNumberView?.setSelectNumListener {
            if (currentMode != FROM_SEARCH) {
                mSwipeLayout.isRefreshing = true
            }
            getTopics(mRequestURL, it)
        }

        if (currentMode == FROM_SEARCH) {
            pendingSearchQuery?.let { startQuery(it) }
        }

        return layout
    }

    // 禁用隐藏fab
    @Deprecated("花里胡哨")
    private fun setUpFabAnimation() {
        fab?.let { fab ->
            mRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                var isFabShowing = true

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) =
                    if (dy > 0) hideFab() else showFab()

                private fun hideFab() {
                    if (isFabShowing) {
                        isFabShowing = false
                        val translation = fab.y.minus(ViewUtil.screenHeight)
                        fab.animate().translationYBy(-translation).setDuration(500)?.start()
                    }
                }

                private fun showFab() {
                    if (!isFabShowing) {
                        isFabShowing = true
                        fab.animate().translationY(0f).setDuration(500).start()
                    }
                }
            })
        }
    }

    fun startQuery(q: SearchOption) {
        if (!::mScrollListener.isInitialized || !::mAdapter.isInitialized || !::flContainer.isInitialized) {
            query = q
            pendingSearchQuery = q
            return
        }
        pendingSearchQuery = null
        query = q
        mScrollListener.restart()
        activity?.runOnUiThread {
            flContainer.hideNoContent()
            mAdapter.clearAndNotify()
            mRecyclerView?.visibility = View.GONE
        }
        makeQuery(query)
    }

    var query: SearchOption = SearchOption("")

    fun showRefresh(show: Boolean) {
        activity?.runOnUiThread {
            if (currentMode != FROM_SEARCH) {
                mSwipeLayout.isRefreshing = show
            }
            mScrollListener.loading = show
            onLoadingChanged?.invoke(show)
            if (!show) {
                showLoadingMore(false)
            }
        }

    }

    private fun loadMoreTopic(currentPage: Int) {

        if (currentMode == FROM_SEARCH) {
            makeQuery(query, currentPage)
        } else {
            getTopics(mRequestURL, currentPage)
        }
    }

    private fun refresh() {
        mScrollListener.restart()
        loadMoreTopic(1)
    }

//  fun updateAvatar(avatar: String) {
//      mAdapter.getList().forEach{
//        it.member?.avatar_normal =  avatar
//      }
//      mAdapter.notifyDataSetChanged()
//  }

    private fun getTopics(requestURL: String, currentPage: Int = 1) {

        if (requestURL == API_HEATED) {
            vCall(API_HEATED)
                .start(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        showRefresh(false)
                        activity?.runOnUiThread {
                            activity?.toast("获取热议主题失败，请重试")
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        showRefresh(false)

                        val str = response.body!!.string()
                        val type = object : TypeToken<List<Topic>>() {}.type
                        val topicList = Gson().fromJson<List<Topic>>(str, type)
                        topicList.forEach {
//                            logi(it.id + ":" + it.title)
                        }

                        activity?.runOnUiThread {
                            if (topicList.isEmpty()) {
                                flContainer.showNoContent()
                                mAdapter.clearAndNotify()
                            } else {
                                mAdapter.updateItems(topicList)
                            }
                        }
                    }
                })

            return
        }


        val url = if (currentMode == FROM_HOME) {
            if (requestURL == "$HTTPS_V2EX_BASE/recent") {
                "$requestURL?p=$currentPage"
            } else {
                requestURL
            }
        } else {
            "$requestURL?p=$currentPage"
        }
        vCall(url)
            .start(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showRefresh(false)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code == 302) {
                        showRefresh(false)
                        if (Objects.equals("/2fa", response.header("Location"))) {
                        }
                    } else if (response.code != 200) {
                        showRefresh(false)
                        dealError(activity, response.code)
                        return
                    }

                    val str = response.body?.string()!!
                    val parser = Parser(str)
                    if (totalPage == 0) {
                        totalPage = parser.getTotalPageForTopics()
                        mScrollListener.totalPage = totalPage
                        if (currentMode == FROM_MEMBER) {
                            val msg = parser.getContentMsg()
                            if (msg.contains("主题列表被隐藏")) {
                                activity?.runOnUiThread {
                                    showRefresh(false)
                                    pageNumberView?.totalNum = 0
                                    flContainer.showNoContent(msg)
                                }
                                return
                            }
                        }

                        activity?.runOnUiThread {
                            if (currentMode == FROM_MEMBER) {
                                val total = parser.getTotalTopics()
                                (activity as MemberActivity?)?.changeTitle(0, total.toString())
                            }
                        }
                    }
                    val topicList = parser.parseTopicLists(currentMode)
//                    logd(topicList)
                    setUIData(topicList)
                }
            })
    }

    private fun setUIData(topicList: List<Topic>) {
        activity?.runOnUiThread {
            if (currentMode != FROM_SEARCH) {
                mSwipeLayout.isRefreshing = false
            }
            mScrollListener.loading = false
            if (topicList.isEmpty()) {
                flContainer.showNoContent()
                mAdapter.clearAndNotify()
            } else {
                flContainer.hideNoContent()
                when (currentMode) {
                    FROM_MEMBER -> {
                        pageNumberView?.totalNum = totalPage
                        topicList.forEach {
                            it.member?.avatar_normal = arguments?.getString(Keys.KEY_AVATAR) ?: ""
//                            logi(it.id + ":" + it.title + " -- " + it.member?.avatar_normal)
                        }
                        if (isEndlessMode) {
                            if (mScrollListener.isRestart()) {
                                topicList.let { mAdapter.updateItems(it) }
                            } else {
                                mScrollListener.success()
                                topicList.let { mAdapter.addAllItems(it) }
                            }
                        } else {
                            topicList.let { mAdapter.updateAllItemsWithoutDiff(it) }
                        }
                    }

                    FROM_NODE -> {
                        if (mScrollListener.isRestart()) {
                            topicList.let { mAdapter.updateItems(it) }
                        } else {
                            mScrollListener.success()
                            topicList.let { mAdapter.addAllItems(it) }
                        }
                    }
                    FROM_HOME -> {
                        if (isEndlessMode) {
                            if (mScrollListener.isRestart()) {
                                topicList.let { mAdapter.updateItems(it) }
                            } else {
                                mScrollListener.success()
                                topicList.let { mAdapter.addAllItems(it) }
                            }
                        } else {
                            topicList.let { mAdapter.updateAllItemsWithoutDiff(it) }
                        }
                    }
                    FROM_SEARCH -> {
                    }
                    FROM_FAVOR -> {
                    }
                }
            }
        }
    }

    fun scrollToTop() = mRecyclerView?.smoothScrollToPosition(0)

    /**
     * nextIndex, not page index, is the item offset
     */
    private fun makeQuery(option: SearchOption, currentPage: Int = 1) {


        val nextIndex = (currentPage - 1) * NUMBER_PER_PAGE

        val url: HttpUrl = HttpUrl.Builder()
            .scheme("https")
            .host(NetManager.API_SEARCH_HOST)
            .addEncodedPathSegments("api/search")

            .addEncodedQueryParameter("q", option.q)
            .addEncodedQueryParameter("from", nextIndex.toString()) // 偏移量, 默认0
            .addEncodedQueryParameter("size", NUMBER_PER_PAGE.toString()) //数量，默认10
            .addEncodedQueryParameter("sort", option.sort)
            .addEncodedQueryParameter("order", option.order)
            .addEncodedQueryParameter("gte", option.gte)
            .addEncodedQueryParameter("lte", option.lte)
            .addEncodedQueryParameter("node", option.node)
            .addEncodedQueryParameter("opterator", "or")
            .addEncodedQueryParameter("username", option.username)
            .build()

        val isFirstPage = nextIndex == 0
        if (isFirstPage) {
            showRefresh(true)
        } else {
            activity?.runOnUiThread { showLoadingMore(true) }
        }
        if (currentMode == FROM_SEARCH && currentPage == 1) {
            activity?.runOnUiThread {
                mRecyclerView?.visibility = View.GONE
                flContainer.hideNoContent()
            }
        }
        HttpHelper.OK_CLIENT
            .newCall(
                Request.Builder()
                    .addHeader("accept", "application/json")
                    .url(url)
                    .build()
            )
            .start(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    if (isFirstPage) {
                        showRefresh(false)
                    } else {
                        activity?.runOnUiThread {
                            mScrollListener.loading = false
                            showLoadingMore(false)
                        }
                    }
                    dealError(context)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
//                    logd(body)
                    val result: SearchResult = Gson().fromJson(body, SearchResult::class.java)
                    if (nextIndex == 0) {
                        result.total?.let {
                            mScrollListener.totalPage = (it / NUMBER_PER_PAGE + 1)
                        }
                    }

                    val topics = result.hits?.map {
                        val topic = Topic()
                        topic.id = it.id.toString()
                        topic.title = it.source?.title.toString()
                        topic.content = it.source?.content
                        topic.created = TimeUtil.toUtcTime(it.source?.created.toString())
                        topic.member = Member().apply { username = it.source?.member.toString() }
                        topic.replies = it.source?.replies
//                        topic.node //  todo需要去做一个node id -> node obj的遍历查找，
                        topic
                    }

                    activity?.runOnUiThread {
                        if (isFirstPage) {
                            showRefresh(false)
                        } else {
                            mScrollListener.loading = false
                            showLoadingMore(false)
                        }
                        if (topics == null) {
                            if (isFirstPage) {
                                mAdapter.clearAndNotify()
                                mRecyclerView?.visibility = View.GONE
                                flContainer.showNoContent(getString(R.string.search_no_results))
                            } else {
                                mScrollListener.totalPage = (currentPage - 1).coerceAtLeast(1)
                                loadingFooterAdapter.setMode(SearchLoadingFooterAdapter.Mode.NoMore)
                            }
                            return@runOnUiThread
                        }
                        if (topics.isEmpty()) {
                            if (isFirstPage) {
                                flContainer.showNoContent(getString(R.string.search_no_results))
                                mAdapter.clearAndNotify()
                                mRecyclerView?.visibility = View.GONE
                            } else {
                                mScrollListener.totalPage = (currentPage - 1).coerceAtLeast(1)
                                loadingFooterAdapter.setMode(SearchLoadingFooterAdapter.Mode.NoMore)
                            }
                        } else {
                            flContainer.hideNoContent()
                            mRecyclerView?.visibility = View.VISIBLE
                            if (nextIndex == 0) {
                                topics.let { mAdapter.updateItems(it) }
                            } else {
                                mScrollListener.success()
                                topics.let { mAdapter.addAllItems(it) }
                            }
                        }
                    }

                }
            })
    }

    private fun showLoadingMore(show: Boolean) {
        if (currentMode == FROM_SEARCH) {
            loadingFooterAdapter.setMode(if (show) SearchLoadingFooterAdapter.Mode.Loading else SearchLoadingFooterAdapter.Mode.Hidden)
        }
        onLoadingMoreChanged?.invoke(show)
    }

    companion object {
        const val NUMBER_PER_PAGE = 10
    }
}

private class SearchLoadingFooterAdapter : RecyclerView.Adapter<SearchLoadingFooterAdapter.FooterHolder>() {
    enum class Mode { Hidden, Loading, NoMore }

    private var mode = Mode.Hidden

    fun setMode(next: Mode) {
        if (mode == next) return
        val oldVisible = mode != Mode.Hidden
        val newVisible = next != Mode.Hidden
        mode = next
        when {
            !oldVisible && newVisible -> notifyItemInserted(0)
            oldVisible && !newVisible -> notifyItemRemoved(0)
            oldVisible && newVisible -> notifyItemChanged(0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterHolder {
        return FooterHolder(ComposeView(parent.context))
    }

    override fun getItemCount(): Int = if (mode == Mode.Hidden) 0 else 1

    override fun onBindViewHolder(holder: FooterHolder, position: Int) {
        holder.view.setContent {
            V2exTheme {
                SearchLoadingFooter(mode)
            }
        }
    }

    class FooterHolder(val view: ComposeView) : RecyclerView.ViewHolder(view)
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SearchLoadingFooter(mode: SearchLoadingFooterAdapter.Mode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (mode) {
            SearchLoadingFooterAdapter.Mode.Loading -> LoadingIndicator()
            SearchLoadingFooterAdapter.Mode.NoMore -> Text(
                text = stringResource(id = R.string.search_no_more_results),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SearchLoadingFooterAdapter.Mode.Hidden -> Unit
        }
    }
}

//0 sumup,1  created

const val SUMUP = "sumup"
const val CREATED = "created"

const val NEW_FIRST = "0"
const val OLD_FIRST = "1"


data class SearchOption(
    val q: String,
    val sort: String = CREATED,
    val order: String = NEW_FIRST,  //0 降序，1 升序
    val gte: String? = null,
    val lte: String? = null,
    val node: String? = null,
    val username: String? = null
)
