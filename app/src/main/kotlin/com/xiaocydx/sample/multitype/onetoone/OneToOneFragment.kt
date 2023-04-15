package com.xiaocydx.sample.multitype.onetoone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
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
        adapter = listAdapter<OneToOneMessage> {
            register(OneToOneTextDelegate().apply {
                doOnSimpleItemClick { showToast("文本类型消息 id = ${it.id}") }
            })
            register(OneToOneImageDelegate().apply {
                doOnSimpleItemClick { showToast("图片类型消息 id = ${it.id}") }
            })
        }.initMessages()

        linear().fixedSize()
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    private fun ListAdapter<OneToOneMessage, *>.initMessages(): Adapter<*> {
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
        return this
    }

    private fun loremText(): String {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Pellentesque commodo leo nec tellus lobortis posuere vel et tellus. " +
                "Quisque viverra nisl quam, ullamcorper fermentum ligula luctus in. " +
                "Integer a nisi et sapien luctus tempus et vel magna. Donec et pharetra ex. "
    }
}