package com.xiaocydx.cxrv.itemselect

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleViewHolder

data class TestItem(val key: String)

class TestAdapter(var data: MutableList<TestItem>) : Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = View(parent.context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, 100)
        }
        return SimpleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = data.size

    fun getItem(position: Int): TestItem = data[position]
}