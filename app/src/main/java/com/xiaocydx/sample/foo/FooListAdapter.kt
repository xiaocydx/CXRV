package com.xiaocydx.sample.foo

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
        return ViewHolder(parent.inflate(layout))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Foo) {
        holder.tvFoo.text = item.name
    }

    override fun getItemViewType(position: Int): Int {
        return toViewType(getItem(position).type)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFoo: TextView = itemView.findViewById(R.id.tvFoo)
    }

    companion object {
        fun toViewType(type: FooType) = type.ordinal
    }
}