package com.xiaocydx.sample.viewpager2.nested

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.concat.withHeader
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.withLayoutParams

/**
 * @author xcc
 * @date 2023/6/22
 */
class NestedPageAdapter : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = RecyclerView(parent.context).apply {
            // 水平方向ViewPager2（Parent）和垂直方向RecyclerView（Child）
            isVp2NestedScrollable = true
            val header = createOuterHeader(parent, viewType)
            adapter = OuterListAdapter(15).withHeader(header)
            overScrollNever()
            linear().fixedSize()
            withLayoutParams(matchParent, matchParent)
        }
        return object : ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = 3

    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) NORMAL_HEADER else LOOP_HEADER
    }

    private fun createOuterHeader(parent: ViewGroup, viewType: Int) = when (viewType) {
        NORMAL_HEADER -> OuterHeader(parent.context)
        LOOP_HEADER -> LoopOuterHeader(parent.context)
        else -> throw IllegalArgumentException()
    }

    private companion object {
        const val NORMAL_HEADER = 0
        const val LOOP_HEADER = 1
    }
}