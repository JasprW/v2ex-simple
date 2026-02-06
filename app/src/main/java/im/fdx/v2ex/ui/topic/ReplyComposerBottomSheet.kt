package im.fdx.v2ex.ui.topic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import im.fdx.v2ex.R
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.utils.extensions.toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ReplyComposerBottomSheet : BottomSheetDialogFragment() {

    private var isSending = false
    private var hasSubmitted = false

    private val topicId: String
        get() = arguments?.getString(ARG_TOPIC_ID).orEmpty()
    private val once: String
        get() = arguments?.getString(ARG_ONCE).orEmpty()
    private val initialText: String
        get() = arguments?.getString(ARG_INITIAL_TEXT).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_reply_composer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etReply = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_reply_content)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel_reply)
        val btnSend = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_send_reply)
        val pbSending = view.findViewById<android.widget.ProgressBar>(R.id.pb_reply_sending)

        etReply.setText(initialText)
        etReply.setSelection(etReply.text?.length ?: 0)
        etReply.requestFocus()

        fun updateSendingState(sending: Boolean) {
            isSending = sending
            btnCancel.isEnabled = !sending
            btnSend.isEnabled = !sending && !etReply.text.isNullOrBlank()
            etReply.isEnabled = !sending
            pbSending.visibility = if (sending) View.VISIBLE else View.GONE
            btnSend.text = if (sending) getString(R.string.sending) else getString(R.string.send)
        }

        etReply.doAfterTextChanged {
            if (!isSending) {
                btnSend.isEnabled = !it.isNullOrBlank()
            }
        }

        btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        btnSend.setOnClickListener {
            if (isSending) {
                return@setOnClickListener
            }
            if (once.isBlank()) {
                context?.toast("发布失败，请刷新页面后重试")
                return@setOnClickListener
            }
            val content = etReply.text?.toString().orEmpty()
            if (content.isBlank()) {
                return@setOnClickListener
            }
            updateSendingState(true)
            val requestBody = FormBody.Builder()
                .add("content", content)
                .add("once", once)
                .build()
            HttpHelper.OK_CLIENT.newCall(
                Request.Builder()
                    .header("Origin", NetManager.HTTPS_V2EX_BASE)
                    .header("Referer", NetManager.HTTPS_V2EX_BASE + "/t/" + topicId)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .url(NetManager.HTTPS_V2EX_BASE + "/t/" + topicId)
                    .post(requestBody)
                    .build()
            ).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        updateSendingState(false)
                        context?.toast("发表回复失败")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    activity?.runOnUiThread {
                        if (response.code == 302) {
                            hasSubmitted = true
                            parentFragmentManager.setFragmentResult(
                                RESULT_KEY,
                                bundleOf(
                                    RESULT_SUCCESS to true,
                                    RESULT_DRAFT to ""
                                )
                            )
                            context?.toast("发表回复成功")
                            dismissAllowingStateLoss()
                        } else {
                            updateSendingState(false)
                            context?.toast("发表回复失败")
                        }
                    }
                }
            })
        }

        updateSendingState(false)
    }

    override fun onDestroyView() {
        if (!hasSubmitted) {
            val draft = view?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_reply_content)
                ?.text
                ?.toString()
                .orEmpty()
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(
                    RESULT_SUCCESS to false,
                    RESULT_DRAFT to draft
                )
            )
        }
        super.onDestroyView()
    }

    companion object {
        const val TAG = "reply_composer_bottom_sheet"
        const val RESULT_KEY = "reply_composer_result"
        const val RESULT_SUCCESS = "result_success"
        const val RESULT_DRAFT = "result_draft"

        private const val ARG_TOPIC_ID = "arg_topic_id"
        private const val ARG_ONCE = "arg_once"
        private const val ARG_INITIAL_TEXT = "arg_initial_text"

        fun newInstance(topicId: String, once: String, initialText: String): ReplyComposerBottomSheet {
            return ReplyComposerBottomSheet().apply {
                arguments = bundleOf(
                    ARG_TOPIC_ID to topicId,
                    ARG_ONCE to once,
                    ARG_INITIAL_TEXT to initialText
                )
            }
        }
    }
}
