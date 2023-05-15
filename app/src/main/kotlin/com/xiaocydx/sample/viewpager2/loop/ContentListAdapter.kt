package com.xiaocydx.sample.viewpager2.loop

import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams

/**
 * @author xcc
 * @date 2023/5/11
 */
class ContentListAdapter : ListAdapter<ContentItem, RecyclerView.ViewHolder>() {
    private val tag = javaClass.simpleName

    override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val textView = AppCompatTextView(parent.context).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFBFD5CC.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            withLayoutParams(matchParent, matchParent)
        }
        return object : RecyclerView.ViewHolder(textView) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ContentItem) {
        Log.d(tag, "onBindViewHolder: " +
                "layoutPosition = ${holder.layoutPosition}，" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}，" +
                "hashCode = ${System.identityHashCode(holder)}")
        (holder.itemView as TextView).text = item.text
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        Log.d(tag, "onViewAttachedToWindow: " +
                "layoutPosition = ${holder.layoutPosition}，" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}，" +
                "hashCode = ${System.identityHashCode(holder)}")
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        Log.d(tag, "onViewDetachedFromWindow: " +
                "layoutPosition = ${holder.layoutPosition}，" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}，" +
                "hashCode = ${System.identityHashCode(holder)}")
    }
}

data class ContentItem(val id: Int, val text: String)