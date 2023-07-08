package com.xiaocydx.sample.viewpager2.nested

import android.os.Parcelable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.setRecycleAllViewsOnDetach
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.databinding.ItemNestedOuterBinding
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2022/4/6
 */
class OuterListAdapter(size: Int) : ListAdapter<OuterItem, OuterHolder>() {
    private val sharedPool = RecyclerView.RecycledViewPool()
    private val savedStates = mutableMapOf<String, Parcelable>()

    init {
        val items = (1..size).map {
            val finalSize = if (it % 2 == 0) size / 2 else size
            OuterItem(
                id = "Outer-$it",
                title = "List-$it",
                data = (1..finalSize).map { value ->
                    InnerItem(id = "Inner-$value", title = "$it-${value}")
                }
            )
        }
        submitList(items)
    }

    override fun areItemsTheSame(oldItem: OuterItem, newItem: OuterItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterHolder {
        val binding = ItemNestedOuterBinding.inflate(
            parent.inflater, parent, false)
        return OuterHolder(sharedPool, binding)
    }

    override fun onBindViewHolder(holder: OuterHolder, item: OuterItem) {
        holder.onBindView(item)
    }

    override fun onViewAttachedToWindow(holder: OuterHolder) {
        holder.onRestoreState(savedStates.remove(holder.item.id))
    }

    override fun onViewDetachedFromWindow(holder: OuterHolder) {
        val state = holder.onSaveState() ?: return
        savedStates[holder.item.id] = state
    }
}

class OuterHolder(
    sharedPool: RecyclerView.RecycledViewPool,
    private val binding: ItemNestedOuterBinding
) : RecyclerView.ViewHolder(binding.root) {
    private val adapter = InnerListAdapter()

    init {
        // 水平方向ViewPager2（Parent）和水平方向RecyclerView（Child）
        binding.rvInner
            .apply { itemAnimator = null }
            .apply { isVp2NestedScrollable = true }
            .apply { setRecycledViewPool(sharedPool) }
            .linear(HORIZONTAL).fixedSize().adapter(adapter)
            .divider(width = 8.dp) { edge(Edge.horizontal()) }
            .setRecycleAllViewsOnDetach(maxScrap = 20, saveState = false)
    }

    fun onBindView(item: OuterItem) {
        binding.tvTitle.text = item.title
        adapter.submitList(item.data)
    }

    fun onSaveState(): Parcelable? {
        return binding.rvInner.layoutManager?.onSaveInstanceState()
    }

    fun onRestoreState(state: Parcelable?): Unit = with(binding.rvInner) {
        if (state == null) {
            layoutManager?.scrollToPosition(0)
        } else {
            layoutManager?.onRestoreInstanceState(state)
        }
    }
}