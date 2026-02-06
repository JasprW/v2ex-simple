package im.fdx.v2ex.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import im.fdx.v2ex.databinding.ItemReplyViewBinding
import im.fdx.v2ex.ui.topic.Reply
import im.fdx.v2ex.utils.extensions.getColorFromAttr
import im.fdx.v2ex.utils.extensions.load

/**
 * 轻量引用预览弹窗，保留旧接口以兼容 GoodTextView 的 PopupListener。
 */
class Popup(mActivity: Context) {

    private val binding: ItemReplyViewBinding =
        ItemReplyViewBinding.inflate(LayoutInflater.from(mActivity), null, false)

    private val popupWindow: PopupWindow = PopupWindow(
        binding.root,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        isOutsideTouchable = true
        elevation = 16f
        isFocusable = true
        setBackgroundDrawable(mActivity.getColorFromAttr(im.fdx.v2ex.R.attr.toolbar_background).toDrawable())
    }

    @SuppressLint("SetTextI18n")
    fun show(v: View, data: Reply, rowNum: Int, clickListener: (Int) -> Unit) {
        binding.tvReplier.text = data.member?.username.orEmpty()
        binding.tvReplyRow.text = "#$rowNum"
        binding.tvReplyTime.text = data.showTime
        binding.tvReplyContent.setGoodText(data.content_rendered, type = typeReply)
        binding.tvReplyContent.maxLines = 5
        binding.ivReplyAvatar.load(data.member?.avatarNormalUrl)
        binding.ivThanks.visibility = View.GONE
        binding.tvThanks.visibility = View.GONE
        binding.ivReply.visibility = View.GONE
        binding.divider.visibility = View.GONE
        binding.flReply.visibility = View.GONE
        binding.flThanks.visibility = View.GONE
        binding.root.setOnClickListener {
            popupWindow.dismiss()
            clickListener(rowNum)
        }

        popupWindow.width = v.width
        popupWindow.showAsDropDown(v, 0, -v.height)
    }

    interface PopupListener {
        fun onClick(v: View, url: String)
    }
}
