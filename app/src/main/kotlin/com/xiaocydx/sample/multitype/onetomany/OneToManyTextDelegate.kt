package com.xiaocydx.sample.multitype.onetomany

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToManyTextDelegate : ViewTypeDelegate<OneToManyMessage, OneToManyTextHolder>() {

    override fun areItemsTheSame(oldItem: OneToManyMessage, newItem: OneToManyMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): OneToManyTextHolder {
        return OneToManyTextHolder(MessageTextLayout(parent.context))
    }

    override fun onBindViewHolder(
        holder: OneToManyTextHolder,
        item: OneToManyMessage
    ) = with(holder.layout) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        tvContent.text = item.content
    }
}

class OneToManyTextHolder(val layout: MessageTextLayout) : RecyclerView.ViewHolder(layout)