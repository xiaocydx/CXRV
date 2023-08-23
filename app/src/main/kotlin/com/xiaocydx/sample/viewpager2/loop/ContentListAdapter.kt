package com.xiaocydx.sample.viewpager2.loop

import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.sample.SimpleViewHolder
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent

/**
 * @author xcc
 * @date 2023/5/11
 */
class ContentListAdapter : ListAdapter<ContentItem, ViewHolder>() {
    private val tag = javaClass.simpleName

    override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = AppCompatTextView(parent.context).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFBFD3FF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            layoutParams(matchParent, matchParent)
        }
        return SimpleViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: ContentItem) {
        Log.d(tag, "onBindViewHolder: " +
                "layoutPosition = ${holder.layoutPosition}，" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}，" +
                "hashCode = ${System.identityHashCode(holder)}")
        (holder.itemView as TextView).text = item.text
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        Log.d(tag, "onViewAttachedToWindow: " +
                "layoutPosition = ${holder.layoutPosition}，" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}，" +
                "hashCode = ${System.identityHashCode(holder)}")
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        Log.d(tag, "onViewDetachedFromWindow: " +
                "layoutPosition = ${holder.layoutPosition}，" +
                "bindingAdapterPosition = ${holder.bindingAdapterPosition}，" +
                "hashCode = ${System.identityHashCode(holder)}")
    }
}

data class ContentItem(val id: Int, val text: String)