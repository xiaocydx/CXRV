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
class OneToOneImageDelegate : ViewTypeDelegate<OneToOneMessage.Image, OneToOneImageDelegate.ViewHolder>() {

    override fun areItemsTheSame(oldItem: OneToOneMessage.Image, newItem: OneToOneMessage.Image): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_message_image))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        item: OneToOneMessage.Image
    ) = with(holder) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        ivContent.setImageResource(item.image)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val ivContent: ImageView = itemView.findViewById(R.id.ivContent)
    }
}