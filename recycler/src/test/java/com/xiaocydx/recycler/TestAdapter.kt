package com.xiaocydx.recycler

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.resolveLayoutParams

/**
 * @author xcc
 * @date 2021/10/15
 */
open class TestAdapter : RecyclerView.Adapter<TestAdapter.ViewHolder>() {
    var items: List<String> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = View(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, 100)
        }
        return ViewHolder(itemView).resolveLayoutParams(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}