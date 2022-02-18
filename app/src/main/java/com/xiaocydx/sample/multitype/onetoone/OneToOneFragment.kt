package com.xiaocydx.sample.multitype.onetoone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.recycler.extension.doOnSimpleItemClick
import com.xiaocydx.recycler.extension.linear
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.submitList
import com.xiaocydx.recycler.multitype.listAdapter
import com.xiaocydx.recycler.multitype.register
import com.xiaocydx.sample.R
import com.xiaocydx.sample.showToast

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
        linear()
        adapter = listAdapter<OneToOneMessage> {
            register(getTextDelegate())
            register(getImageDelegate())
        }.initMessages()
        overScrollMode = OVER_SCROLL_NEVER
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    private fun getTextDelegate(): OneToOneTextDelegate {
        return OneToOneTextDelegate().doOnSimpleItemClick {
            showToast("文本类型消息 id = ${it.id}")
        }
    }

    private fun getImageDelegate(): OneToOneImageDelegate {
        return OneToOneImageDelegate().doOnSimpleItemClick {
            showToast("图片类型消息 id = ${it.id}")
        }
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