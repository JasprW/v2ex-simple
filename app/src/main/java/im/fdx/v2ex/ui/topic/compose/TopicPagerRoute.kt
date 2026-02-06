package im.fdx.v2ex.ui.topic.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import im.fdx.v2ex.ui.main.Topic
import kotlin.math.absoluteValue


data class TopicPageItem(
    val topicId: String,
    val topic: Topic?,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopicPagerRoute(
    pages: List<TopicPageItem>,
    initialPage: Int,
    pagerEnabled: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenMember: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenPhotos: (List<String>, Int) -> Unit,
) {
    val pageCount = pages.size
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
        pageCount = { pageCount },
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = pagerEnabled && pageCount > 1,
    ) { page ->
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue
        val scale = lerp(0.95f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
        val alpha = lerp(0.75f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

        TopicDetailRoute(
            topicId = pages[page].topicId,
            initialTopic = pages[page].topic,
            onBack = onBack,
            onLogin = onLogin,
            onOpenMember = onOpenMember,
            onOpenNode = onOpenNode,
            onOpenTopic = onOpenTopic,
            onOpenPhotos = onOpenPhotos,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f - pageOffset.coerceIn(0f, 1f))
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        )
    }
}
