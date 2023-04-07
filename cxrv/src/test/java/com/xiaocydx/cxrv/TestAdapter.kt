package com.xiaocydx.cxrv

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleViewHolder

/**
 * @author xcc
 * @date 2021/10/15
 */
internal open class TestAdapter : RecyclerView.Adapter<ViewHolder>() {
    var items: List<String> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = View(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, 100)
        }
        return SimpleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = items.size
}