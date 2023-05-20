package com.xiaocydx.sample.multitype.onetoone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.*

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToOneFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        val textDelegate = OneToOneTextDelegate()
        val imageDelegate = OneToOneImageDelegate()
        textDelegate.doOnSimpleItemClick { showToast("文本类型消息 id = ${it.id}") }
        imageDelegate.doOnSimpleItemClick { showToast("图片类型消息 id = ${it.id}") }

        linear().fixedSize()
        adapter(listAdapter<OneToOneMessage> {
            register(textDelegate)
            register(imageDelegate)
            listAdapter.initOneToOneMessages()
        })
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    private fun ListAdapter<OneToOneMessage, *>.initOneToOneMessages() = apply {
        val username = "用户A"
        val avatar = R.mipmap.ic_launcher_round
        val messages = (1..10).map {
            if (it % 2 != 0) {
                OneToOneMessage.Text(id = it, avatar = avatar,
                    username = username, content = "文本消息$it - ${loremText()}")
            } else {
                OneToOneMessage.Image(id = it, avatar = avatar,
                    username = username, image = R.mipmap.ic_launcher)
            }
        }
        submitList(messages)
    }

    private fun loremText(): String {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Pellentesque commodo leo nec tellus lobortis posuere vel et tellus. " +
                "Quisque viverra nisl quam, ullamcorper fermentum ligula luctus in. " +
                "Integer a nisi et sapien luctus tempus et vel magna. Donec et pharetra ex. "
    }
}