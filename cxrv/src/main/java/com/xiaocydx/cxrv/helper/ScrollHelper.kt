package com.xiaocydx.cxrv.helper

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.itemvisible.isFirstItemCompletelyVisible

/**
 * 列表更新时调用[RecyclerView.scrollToPosition]的帮助类
 *
 * @author xcc
 * @date 2022/4/27
 */
class ScrollHelper : ListUpdateHelper() {

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (positionStart == 0
                && previousItemCount != 0
                && rv?.isFirstItemCompletelyVisible == true) {
            // 即使Adapter是ConcatAdapter的元素，也不会影响该判断逻辑
            // TODO: 2022/6/12 观察对StaggeredGridLayoutManager的影响
            rv?.scrollToPosition(0)
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if ((fromPosition == 0 || toPosition == 0)
                && rv?.isFirstItemCompletelyVisible == true) {
            // 即使Adapter是ConcatAdapter的元素，也不会影响该判断逻辑
            rv?.scrollToPosition(0)
        }
    }
}