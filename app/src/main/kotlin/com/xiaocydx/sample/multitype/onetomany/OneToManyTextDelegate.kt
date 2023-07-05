package com.xiaocydx.sample.multitype.onetomany

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
class OneToManyTextDelegate : ViewTypeDelegate<OneToManyMessage, OneToManyTextHolder>() {

    override fun areItemsTheSame(oldItem: OneToManyMessage, newItem: OneToManyMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): OneToManyTextHolder {
        return OneToManyTextHolder(parent.inflate(R.layout.item_message_text))
    }

    override fun onBindViewHolder(
        holder: OneToManyTextHolder,
        item: OneToManyMessage
    ) = with(holder) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        tvContent.text = item.content
    }
}

class OneToManyTextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
    val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
    val tvContent: TextView = itemView.findViewById(R.id.tvContent)
}