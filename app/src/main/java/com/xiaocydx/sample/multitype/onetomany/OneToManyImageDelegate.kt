package com.xiaocydx.sample.multitype.onetomany

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToManyImageDelegate :
    ViewTypeDelegate<OneToManyMessage, OneToManyImageDelegate.ViewHolder>() {

    override fun areItemsTheSame(oldItem: OneToManyMessage, newItem: OneToManyMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val layout = MessageImageLayout(parent.context)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        item: OneToManyMessage
    ) = with(holder.layout) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        ivContent.setImageResource(item.image)
    }

    class ViewHolder(val layout: MessageImageLayout) : RecyclerView.ViewHolder(layout)
}