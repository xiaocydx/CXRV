package com.xiaocydx.sample.multitype.onetomany

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.*

class OneToManyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        adapter = listAdapter<OneToManyMessage> {
            register(OneToManyTextDelegate().apply {
                typeLinker { it.type == "text" }
                doOnSimpleItemClick { showToast("文本类型消息 id = ${it.id}") }
            })

            register(OneToManyImageDelegate().apply {
                typeLinker { it.type == "image" }
                doOnSimpleItemClick { showToast("图片类型消息 id = ${it.id}") }
            })
        }.initMessages()

        linear().fixedSize()
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    private fun ListAdapter<OneToManyMessage, *>.initMessages(): RecyclerView.Adapter<*> {
        val username = "用户A"
        val avatar = R.mipmap.ic_launcher_round
        val messages = (1..10).map {
            if (it % 2 != 0) {
                OneToManyMessage(id = it, avatar = avatar,
                    username = username, type = "text", content = "文本消息$it - ${loremText()}")
            } else {
                OneToManyMessage(id = it, avatar = avatar,
                    username = username, type = "image", image = R.mipmap.ic_launcher)
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