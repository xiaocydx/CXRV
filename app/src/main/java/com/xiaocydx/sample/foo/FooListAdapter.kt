package com.xiaocydx.sample.foo

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.sample.R

/**
 * @author xcc
 * @date 2022/2/17
 */
class FooListAdapter : ListAdapter<Foo, FooListAdapter.ViewHolder>() {

    override fun areItemsTheSame(oldItem: Foo, newItem: Foo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = when (viewType) {
            FooType.TYPE1.ordinal -> R.layout.item_foo_type1
            FooType.TYPE2.ordinal -> R.layout.item_foo_type2
            else -> throw IllegalArgumentException()
        }
        val holder = ViewHolder(parent.inflate(layout))
        val isStaggered = recyclerView?.layoutManager is StaggeredGridLayoutManager
        if (isStaggered) {
            // 兼容瀑布流的测量逻辑，确保itemView高度不会被“挤压”
            val initialHeight = holder.itemView.layoutParams.height
            holder.itemView.updateLayoutParams { height = WRAP_CONTENT }
            holder.tvFoo.updateLayoutParams { height = initialHeight }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Foo) {
        holder.tvFoo.text = item.name
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFoo: TextView = itemView.findViewById(R.id.tvFoo)
    }
}