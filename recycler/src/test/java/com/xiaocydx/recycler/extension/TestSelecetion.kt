package com.xiaocydx.recycler.extension

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

data class TestItem(val key: String)

class TestAdapter(
    var data: MutableList<TestItem>
) : Adapter<TestAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = View(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
        }
        return ViewHolder(itemView).resolveLayoutParams(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = data.size

    fun getItem(position: Int): TestItem = data[position]

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}