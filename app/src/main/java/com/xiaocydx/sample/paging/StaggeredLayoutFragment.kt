package com.xiaocydx.sample.paging

import com.xiaocydx.recycler.extension.divider
import com.xiaocydx.recycler.extension.staggered
import com.xiaocydx.recycler.paging.emitAll
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.launchRepeatOnViewLifecycle
import com.xiaocydx.sample.paging.config.pagingSwipeRefresh

/**
 * @author xcc
 * @date 2022/2/17
 */
class StaggeredLayoutFragment : PagingFragment() {

    override fun initView() {
        rvPaging
            .staggered(spanCount = 3)
            .divider {
                height = 5.dp
                width = 5.dp
                color = 0xFF9DAA8F.toInt()
                verticalEdge = true
                horizontalEdge = true
            }
            // .paging(adapter) // 无拖拽刷新
            .pagingSwipeRefresh(adapter)
    }

    override fun initObserve() {
        super.initObserve()
        viewModel.enableMultiTypeFoo()
        launchRepeatOnViewLifecycle {
            adapter.emitAll(viewModel.flow)
        }
    }
}