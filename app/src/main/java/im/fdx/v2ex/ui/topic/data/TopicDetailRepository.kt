package im.fdx.v2ex.ui.topic.data

import com.google.gson.JsonParser
import im.fdx.v2ex.database.DbHelper
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.topic.MyReply
import im.fdx.v2ex.ui.topic.Reply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

data class TopicPageData(
    val topic: Topic?,
    val replies: List<Reply>,
    val currentPage: Int,
    val totalPages: Int,
    val once: String?,
    val isFavored: Boolean,
    val isThanked: Boolean,
    val isIgnored: Boolean,
)

interface TopicDetailRepository {
    suspend fun loadTopicPage(topicId: String, page: Int): TopicPageData
    suspend fun toggleFavorite(topicId: String, once: String, favored: Boolean)
    suspend fun thankTopic(topicId: String, once: String)
    suspend fun ignoreTopic(topicId: String, once: String, ignored: Boolean)
    suspend fun submitReply(topicId: String, once: String, content: String)
    suspend fun thankReply(replyId: String, once: String): String?
    suspend fun hideReply(replyId: String, once: String)
    suspend fun loadDraft(topicId: String): String
    suspend fun saveDraft(topicId: String, content: String)
}

class TopicDetailRepositoryImpl : TopicDetailRepository {

    override suspend fun loadTopicPage(topicId: String, page: Int): TopicPageData = withContext(Dispatchers.IO) {
        val response = vCall("${NetManager.HTTPS_V2EX_BASE}/t/$topicId?p=$page").execute()
        response.use {
            when (it.code) {
                200 -> Unit
                302 -> throw TopicDetailAuthException
                else -> throw IOException("Load topic failed: ${it.code}")
            }
            val body = it.body?.string().orEmpty()
            val parser = Parser(body)
            val pageValue = parser.getPageValue()
            val parsedCurrentPage = pageValue.getOrElse(0) { page }.takeIf { value -> value > 0 } ?: page
            val parsedTotalPage = pageValue.getOrElse(1) { 1 }.takeIf { value -> value > 0 } ?: 1

            TopicPageData(
                topic = if (page == 1) parser.parseResponseToTopic(topicId) else null,
                replies = parser.getReplies(),
                currentPage = parsedCurrentPage,
                totalPages = parsedTotalPage,
                once = parser.getOnceNum().takeUnless { value -> value == "0" },
                isFavored = parser.isTopicFavored(),
                isThanked = parser.isTopicThanked(),
                isIgnored = parser.isIgnored(),
            )
        }
    }

    override suspend fun toggleFavorite(topicId: String, once: String, favored: Boolean) = withContext(Dispatchers.IO) {
        val prefix = if (favored) "un" else ""
        val response = vCall("${NetManager.HTTPS_V2EX_BASE}/${prefix}favorite/topic/$topicId?once=$once").execute()
        response.use {
            if (it.code != 302) {
                throw IOException("Toggle favorite failed: ${it.code}")
            }
        }
    }

    override suspend fun thankTopic(topicId: String, once: String) = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("once", once).build()
        val request = Request.Builder()
            .url("${NetManager.HTTPS_V2EX_BASE}/thank/topic/$topicId")
            .post(body)
            .build()
        val response = HttpHelper.OK_CLIENT.newCall(request).execute()
        response.use {
            if (it.code != 200) {
                throw IOException("Thank topic failed: ${it.code}")
            }
        }
    }

    override suspend fun ignoreTopic(topicId: String, once: String, ignored: Boolean) = withContext(Dispatchers.IO) {
        val prefix = if (ignored) "un" else ""
        val response = vCall("${NetManager.HTTPS_V2EX_BASE}/${prefix}ignore/topic/$topicId?once=$once").execute()
        response.use {
            if (it.code != 302) {
                throw IOException("Ignore topic failed: ${it.code}")
            }
        }
    }

    override suspend fun submitReply(topicId: String, once: String, content: String) = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("content", content)
            .add("once", once)
            .build()

        val request = Request.Builder()
            .header("Origin", NetManager.HTTPS_V2EX_BASE)
            .header("Referer", "${NetManager.HTTPS_V2EX_BASE}/t/$topicId")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .url("${NetManager.HTTPS_V2EX_BASE}/t/$topicId")
            .post(requestBody)
            .build()
        val response = HttpHelper.OK_CLIENT.newCall(request).execute()
        response.use {
            if (it.code != 302) {
                throw IOException("Submit reply failed: ${it.code}")
            }
        }
    }

    override suspend fun thankReply(replyId: String, once: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${NetManager.HTTPS_V2EX_BASE}/thank/reply/$replyId")
            .post(FormBody.Builder().add("once", once).build())
            .build()
        val response = HttpHelper.OK_CLIENT.newCall(request).execute()
        response.use {
            if (it.code != 200) {
                throw IOException("Thank reply failed: ${it.code}")
            }
            val body = it.body?.string().orEmpty()
            if (body.isBlank()) {
                return@withContext null
            }
            val root = JsonParser.parseString(body).asJsonObject
            val success = root.get("success")?.asBoolean == true
            if (!success) {
                throw IOException("Thank reply failed: response unsuccessful")
            }
            root.get("once")?.asString
        }
    }

    override suspend fun hideReply(replyId: String, once: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${NetManager.HTTPS_V2EX_BASE}/ignore/reply/$replyId")
            .post(FormBody.Builder().add("once", once).build())
            .build()
        val response = HttpHelper.OK_CLIENT.newCall(request).execute()
        response.use {
            if (it.code != 200) {
                throw IOException("Hide reply failed: ${it.code}")
            }
        }
    }

    override suspend fun loadDraft(topicId: String): String {
        return DbHelper.db.myReplyDao().getMyReplyById(topicId)?.content.orEmpty()
    }

    override suspend fun saveDraft(topicId: String, content: String) {
        DbHelper.db.myReplyDao().insert(MyReply(topicId = topicId, content = content))
    }
}

object TopicDetailAuthException : IOException("Authentication required")
