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
import im.fdx.v2ex.ui.member.compose.MemberTopicsRoute
import im.fdx.v2ex.ui.topic.TopicActivity
import im.fdx.v2ex.utils.Keys

class MemberTopicComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val username = arguments?.getString(Keys.KEY_USERNAME).orEmpty()
        val avatar = arguments?.getString(Keys.KEY_AVATAR).orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                V2exTheme {
                    MemberTopicsRoute(
                        username = username,
                        avatar = avatar,
                        onOpenTopic = { topicId ->
                            startActivity(Intent(requireContext(), TopicActivity::class.java).apply {
                                putExtra(Keys.KEY_TOPIC_ID, topicId)
                            })
                        },
                        onTopicsCountChanged = { count ->
                            (activity as? MemberActivity)?.changeTitle(0, count)
                        },
                    )
                }
            }
        }
    }
}
