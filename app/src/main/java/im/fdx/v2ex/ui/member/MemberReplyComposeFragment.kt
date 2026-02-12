package im.fdx.v2ex.ui.member

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.member.compose.MemberRepliesRoute
import im.fdx.v2ex.ui.topic.TopicActivity
import im.fdx.v2ex.utils.Keys

class MemberReplyComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val username = arguments?.getString(Keys.KEY_USERNAME).orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                V2exTheme {
                    MemberRepliesRoute(
                        username = username,
                        onOpenTopic = { topicId ->
                            startActivity(Intent(requireContext(), TopicActivity::class.java).apply {
                                putExtra(Keys.KEY_TOPIC_ID, topicId)
                            })
                        },
                        onRepliesCountChanged = { count ->
                            (activity as? MemberActivity)?.changeTitle(1, count)
                        },
                    )
                }
            }
        }
    }
}
