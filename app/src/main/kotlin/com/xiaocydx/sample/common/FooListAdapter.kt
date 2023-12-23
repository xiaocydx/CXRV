package com.xiaocydx.sample.common

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.sample.R

/**
 * @author xcc
 * @date 2022/2/17
 */
class FooListAdapter : ListAdapter<Foo, FooListAdapter.ViewHolder>() {
    private val type1Size = 80.dp
    private val type2Size = 140.dp
    private val RecyclerView.orientation: Int
        get() = when (val lm = layoutManager) {
            is LinearLayoutManager -> lm.orientation
            is StaggeredGridLayoutManager -> lm.orientation
            else -> -1
        }

    override fun areItemsTheSame(oldItem: Foo, newItem: Foo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val resId = when (viewType) {
            FooType.TYPE1.ordinal -> R.layout.item_foo_type1
            FooType.TYPE2.ordinal -> R.layout.item_foo_type2
            else -> throw IllegalArgumentException()
        }
        return ViewHolder(parent.inflate(resId))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Foo) {
        val size = when (holder.itemViewType) {
            FooType.TYPE1.ordinal -> type1Size
            FooType.TYPE2.ordinal -> type2Size
            else -> throw IllegalArgumentException()
        }
        val rv = requireNotNull(recyclerView)
        val isVertical = rv.orientation == RecyclerView.VERTICAL
        holder.itemView.updateLayoutParams {
            width = if (isVertical) matchParent else size
            height = if (isVertical) size else matchParent
        }
        holder.tvFoo.updateLayoutParams {
            width = matchParent
            height = matchParent
        }
        holder.tvFoo.text = item.name
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFoo: TextView = itemView.findViewById(R.id.tvFoo)
    }
}