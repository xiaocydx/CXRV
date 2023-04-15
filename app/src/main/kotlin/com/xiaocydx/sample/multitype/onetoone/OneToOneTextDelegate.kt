package com.xiaocydx.sample.multitype.onetoone

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.sample.R

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToOneTextDelegate : ViewTypeDelegate<OneToOneMessage.Text, OneToOneTextDelegate.ViewHolder>() {

    override fun areItemsTheSame(oldItem: OneToOneMessage.Text, newItem: OneToOneMessage.Text): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_message_text))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        item: OneToOneMessage.Text
    ) = with(holder) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        tvContent.text = item.content
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
    }
}