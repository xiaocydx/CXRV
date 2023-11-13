@file:Suppress("FunctionName")

package com.xiaocydx.sample.multitype.onetomany

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingDelegate
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ItemMessageImageBinding
import com.xiaocydx.sample.databinding.ItemMessageNotSupportBinding
import com.xiaocydx.sample.databinding.ItemMessageTextBinding
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.multitype.onetomany.OneToManyMessage.Companion.TYPE_IMAGE
import com.xiaocydx.sample.multitype.onetomany.OneToManyMessage.Companion.TYPE_TEXT
import com.xiaocydx.sample.multitype.onetomany.OneToManyMessage.Companion.TYPE_UNKNOWN
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.snackbar

/**
 * 一对多类型关系示例代码
 *
 * @author xcc
 * @date 2022/2/17
 */
class OneToManyFragment : Fragment() {

    /**
     * 可以通过以下方式构建[ViewTypeDelegate]：
     * 1. 继承[ViewTypeDelegate]。
     * 2. 继承[BindingDelegate]。
     * 3. 调用[bindingDelegate]。
     * 示例代码的列表比较简单，因此选择第3种方式。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())
        .layoutParams(matchParent, matchParent)
        .overScrollNever().linear().fixedSize()
        .adapter(listAdapter<OneToManyMessage> {
            listAdapter.submitList(messageList())
            register(OneToManyTextDelegate()) { it.type == TYPE_TEXT }
            register(OneToManyImageDelegate()) { it.type == TYPE_IMAGE }
            register(OneToManyNotSupportDelegate()) { true }
        })

    private fun OneToManyTextDelegate() = bindingDelegate(
        uniqueId = OneToManyMessage::id,
        inflate = ItemMessageTextBinding::inflate
    ) {
        onBindView { item ->
            ivAvatar.setImageResource(item.avatar)
            tvUsername.text = item.username
            tvContent.text = item.content
        }
        doOnItemClick { holder, item ->
            holder.itemView.snackbar()
                .setText("文本类型消息 id = ${item.id}")
                .show()
        }
    }

    private fun OneToManyImageDelegate() = bindingDelegate(
        uniqueId = OneToManyMessage::id,
        inflate = ItemMessageImageBinding::inflate
    ) {
        onBindView { item ->
            ivAvatar.setImageResource(item.avatar)
            tvUsername.text = item.username
            ivContent.setImageResource(item.image)
        }
        doOnItemClick { holder, item ->
            holder.itemView.snackbar()
                .setText("图片类型消息 id = ${item.id}")
                .show()
        }
    }

    /**
     * 若`type`是服务端提供的类型字段，则后续迭代可能会对`type`新增类型值，
     * 因此需要考虑一对多类型关系的兼容性，避免检查机制抛出异常，导致程序崩溃。
     */
    private fun OneToManyNotSupportDelegate() = bindingDelegate(
        uniqueId = OneToManyMessage::id,
        inflate = ItemMessageNotSupportBinding::inflate
    ) {
        onBindView { item ->
            ivAvatar.setImageResource(item.avatar)
            tvUsername.text = item.username
        }
        doOnItemClick { holder, item ->
            holder.itemView.snackbar()
                .setText("未知类型消息 id = ${item.id}")
                .show()
        }
    }

    private fun messageList(): List<OneToManyMessage> {
        val username = "用户A"
        val avatar = R.mipmap.ic_launcher_round
        return (1..10).map { id ->
            var type = ""
            var content = ""
            var image = 0
            when {
                id == 1 -> type = TYPE_UNKNOWN
                id % 2 != 0 -> {
                    type = TYPE_TEXT
                    content = "文本消息$id - ${loremText()}"
                }
                else -> {
                    type = TYPE_IMAGE
                    image = R.mipmap.ic_launcher
                }
            }
            OneToManyMessage(id, avatar, username, type, content, image)
        }
    }

    private fun loremText(): String {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Pellentesque commodo leo nec tellus lobortis posuere vel et tellus. " +
                "Quisque viverra nisl quam, ullamcorper fermentum ligula luctus in. " +
                "Integer a nisi et sapien luctus tempus et vel magna. Donec et pharetra ex. "
    }
}