@file:Suppress("FunctionName")

package com.xiaocydx.sample.viewpager2.nested

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.setRecycleAllViewsOnDetach
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.insertItems
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding
import com.xiaocydx.sample.databinding.ItemNestedOuterBinding

fun NestedPageOuterListAdapter(size: Int) = bindingAdapter(
    uniqueId = OuterItem::id,
    inflate = ItemNestedOuterBinding::inflate
) {
    submitList((1..size).map {
        val finalSize = if (it % 2 == 0) size / 2 else size
        OuterItem(
            id = "Outer-$it", title = "List-$it",
            data = (1..finalSize).map { value ->
                InnerItem(id = "Inner-$value", title = "$it-${value}")
            }
        )
    })

    val sharedPool = RecyclerView.RecycledViewPool()
    val savedStates = mutableMapOf<String, Parcelable?>()
    onCreateView {
        // 水平方向ViewPager2（Parent）和水平方向RecyclerView（Child）
        rvInner
            .apply { itemAnimator = null }
            .apply { isVp2NestedScrollable = true }
            .apply { setRecycledViewPool(sharedPool) }
            .divider(width = 8.dp) { edge(Edge.horizontal()) }
            .linear(HORIZONTAL).adapter(NestedPageInnerListAdapter())
            .setRecycleAllViewsOnDetach(maxScrap = 20, saveState = false)
    }
    onBindView { item ->
        tvTitle.text = item.title
        rvInner.adapter.let {
            @Suppress("UNCHECKED_CAST")
            it as? ListAdapter<InnerItem, *>
        }?.apply {
            clear()
            insertItems(item.data)
        }
    }
    onViewAttachedToWindow {
        val state = savedStates.remove(holder.item.id)
        if (state == null) {
            rvInner.layoutManager?.scrollToPosition(0)
        } else {
            rvInner.layoutManager?.onRestoreInstanceState(state)
        }
    }
    onViewDetachedFromWindow {
        val state = rvInner.layoutManager?.onSaveInstanceState()
        savedStates[holder.item.id] = state
    }
}

private fun NestedPageInnerListAdapter() = bindingAdapter(
    uniqueId = InnerItem::id,
    inflate = ItemNestedInnerBinding::inflate
) {
    onBindView { root.text = it.title }
}