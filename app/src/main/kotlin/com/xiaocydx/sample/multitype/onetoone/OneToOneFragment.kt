@file:Suppress("FunctionName")

package com.xiaocydx.sample.multitype.onetoone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.cxrv.binding.BindingDelegate
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ItemMessageImageBinding
import com.xiaocydx.sample.databinding.ItemMessageTextBinding

/**
 * 一对一类型关系示例代码
 *
 * @author xcc
 * @date 2022/2/17
 */
class OneToOneFragment : Fragment() {

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
        .linear().adapter(listAdapter<OneToOneMessage> {
            register(OneToOneTextDelegate())
            register(OneToOneImageDelegate())
            submitList(messageList())
        })

    private fun OneToOneTextDelegate() = bindingDelegate(
        uniqueId = OneToOneMessage.Text::id,
        inflate = ItemMessageTextBinding::inflate
    ) {
        onBindView { item ->
            ivAvatar.setImageResource(item.avatar)
            tvUsername.text = item.username
            tvContent.text = item.content
        }
        doOnItemClick { item ->
            snackbar().setText("文本类型消息 id = ${item.id}").show()
        }
    }

    private fun OneToOneImageDelegate() = bindingDelegate(
        uniqueId = OneToOneMessage.Image::id,
        inflate = ItemMessageImageBinding::inflate
    ) {
        onBindView { item ->
            ivAvatar.setImageResource(item.avatar)
            tvUsername.text = item.username
            ivContent.setImageResource(item.image)
        }
        doOnItemClick { item ->
            snackbar().setText("图片类型消息 id = ${item.id}").show()
        }
    }

    private fun messageList(): List<OneToOneMessage> {
        val username = "用户A"
        val avatar = R.mipmap.ic_launcher_round
        return (1..10).map {
            if (it % 2 != 0) {
                OneToOneMessage.Text(id = it, avatar = avatar,
                    username = username, content = "文本消息$it - ${loremText()}")
            } else {
                OneToOneMessage.Image(id = it, avatar = avatar,
                    username = username, image = R.mipmap.ic_launcher)
            }
        }
    }

    private fun loremText(): String {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Pellentesque commodo leo nec tellus lobortis posuere vel et tellus. " +
                "Quisque viverra nisl quam, ullamcorper fermentum ligula luctus in. " +
                "Integer a nisi et sapien luctus tempus et vel magna. Donec et pharetra ex. "
    }
}