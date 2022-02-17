package com.xiaocydx.sample.multitype.onetoone

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2022/2/17
 */
class OneToOneTextDelegate : ViewTypeDelegate<OneToOneMessage.Text, OneToOneTextDelegate.ViewHolder>() {

    override fun areItemsTheSame(oldItem: OneToOneMessage.Text, newItem: OneToOneMessage.Text): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.item_message))
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
        val flContainer: FrameLayout = itemView.findViewById(R.id.flContainer)
        val tvContent: TextView = AppCompatTextView(itemView.context).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setBackgroundColor(0xFF5998FF.toInt())
            setPadding(12.dp)
            includeFontPadding = false
            layoutParams = MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            flContainer.addView(this)
        }
    }
}