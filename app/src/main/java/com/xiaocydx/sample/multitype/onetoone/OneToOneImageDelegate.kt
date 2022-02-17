package com.xiaocydx.sample.multitype.onetoone

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToOneImageDelegate : ViewTypeDelegate<OneToOneMessage.Image, OneToOneImageDelegate.ViewHolder>() {

    override fun areItemsTheSame(oldItem: OneToOneMessage.Image, newItem: OneToOneMessage.Image): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_message))
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
        val flContainer: FrameLayout = itemView.findViewById(R.id.flContainer)
        val ivContent: ImageView = AppCompatImageView(itemView.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = MarginLayoutParams(150.dp, 150.dp)
        }.also(flContainer::addView)
    }
}