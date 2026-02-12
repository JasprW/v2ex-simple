package im.fdx.v2ex.ui.node

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import im.fdx.v2ex.pref
import im.fdx.v2ex.utils.Keys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class AllNodesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AllNodesUiState())
    val uiState: StateFlow<AllNodesUiState> = _uiState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var allNodes: List<Node> = emptyList()

    fun initialize(onError: (Int) -> Unit) {
        val cacheNodes = readCache()
        if (cacheNodes.isNotEmpty()) {
            allNodes = cacheNodes
            applyFilter(_uiState.value.query)
        } else {
            refresh(onError)
        }
    }

    fun refresh(onError: (Int) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        vCall(NetManager.URL_ALL_NODE_WEB).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onError(-1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    mainHandler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onError(response.code)
                    }
                    return
                }
                val html = response.body?.string().orEmpty()
                val nodes = Parser(html).getAllNode()
                if (nodes.isNotEmpty()) {
                    writeCache(nodes)
                }
                mainHandler.post {
                    allNodes = nodes
                    applyFilter(_uiState.value.query)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        })
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) {
            allNodes
        } else {
            allNodes.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.title_alternative.contains(query, ignoreCase = true)
            }
        }

        val map = listToMap(filtered)
        val sections = map.entries.map { NodeSection(it.key, it.value) }
        _uiState.value = _uiState.value.copy(
            sections = sections,
            isEmpty = sections.isEmpty(),
        )
    }

    private fun readCache(): List<Node> {
        val time = pref.getLong(Keys.PREF_ALL_NODE_DATA_TIME, 0L)
        val isExpired = System.currentTimeMillis() - time > 24 * 60 * 60 * 1000
        if (time == 0L || isExpired) return emptyList()
        val cache = pref.getString(Keys.PREF_ALL_NODE_DATA, "").orEmpty()
        if (cache.isBlank()) return emptyList()
        val type = object : TypeToken<List<Node>>() {}.type
        return runCatching { Gson().fromJson<List<Node>>(cache, type) }.getOrElse { emptyList() }
    }

    private fun writeCache(nodes: List<Node>) {
        pref.edit()
            .putString(Keys.PREF_ALL_NODE_DATA, Gson().toJson(nodes))
            .putLong(Keys.PREF_ALL_NODE_DATA_TIME, System.currentTimeMillis())
            .apply()
    }
}
