package com.xiaocydx.sample.multitype.onetomany

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.resolveLayoutParams
import com.xiaocydx.recycler.multitype.ViewTypeDelegate

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToManyTextDelegate : ViewTypeDelegate<OneToManyMessage, OneToManyTextDelegate.ViewHolder>() {

    override fun areItemsTheSame(oldItem: OneToManyMessage, newItem: OneToManyMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val layout = MessageTextLayout(parent.context)
        return ViewHolder(layout).resolveLayoutParams(parent)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        item: OneToManyMessage
    ) = with(holder.layout) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        tvContent.text = item.content
    }

    class ViewHolder(val layout: MessageTextLayout) : RecyclerView.ViewHolder(layout)
}