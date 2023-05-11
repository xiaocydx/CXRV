package com.xiaocydx.sample.viewpager2.pageloop

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
        (holder.itemView as TextView).text = item.text
        println("test -> laypos = ${holder.layoutPosition}, bindingPos = ${holder.bindingAdapterPosition}, hashCode = ${holder.hashCode()}")
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.itemAnimator = null
        recyclerView.layoutManager?.isItemPrefetchEnabled = false
    }
}

data class ContentItem(val id: Int, val text: String)