package com.xiaocydx.sample.viewpager2.nested

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.concat.withHeader
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.SimpleViewHolder
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever

/**
 * 不是所有场景都需要使用[ListAdapter]，应当结合需求选择合适的适配器
 *
 * @author xcc
 * @date 2023/6/22
 */
class NestedPageAdapter : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val pageHeader = when (viewType) {
            NORMAL_HEADER -> NestedPageHeader(parent.context)
            LOOP_HEADER -> LoopNestedPageHeader(parent.context)
            else -> throw IllegalArgumentException()
        }.toAdapter()
        val pageList = NestedPageOuterListAdapter(15)

        // 水平方向ViewPager2（Parent）和垂直方向RecyclerView（Child）
        val pageView = RecyclerView(parent.context)
            .apply { isVp2NestedScrollable = true }
            .layoutParams(matchParent, matchParent)
            .overScrollNever().linear().fixedSize()
            .adapter(pageList.withHeader(pageHeader))
        return SimpleViewHolder(pageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = 3

    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) NORMAL_HEADER else LOOP_HEADER
    }

    private companion object {
        const val NORMAL_HEADER = 0
        const val LOOP_HEADER = 1
    }
}