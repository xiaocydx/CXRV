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
class OneToManyImageDelegate : ViewTypeDelegate<OneToManyMessage, OneToManyImageHolder>() {

    override fun areItemsTheSame(oldItem: OneToManyMessage, newItem: OneToManyMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): OneToManyImageHolder {
        return OneToManyImageHolder(parent.inflate(R.layout.item_message_image))
    }

    override fun onBindViewHolder(
        holder: OneToManyImageHolder,
        item: OneToManyMessage
    ) = with(holder) {
        ivAvatar.setImageResource(item.avatar)
        tvUsername.text = item.username
        ivContent.setImageResource(item.image)
    }
}

class OneToManyImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
    val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
    val ivContent: ImageView = itemView.findViewById(R.id.ivContent)
}