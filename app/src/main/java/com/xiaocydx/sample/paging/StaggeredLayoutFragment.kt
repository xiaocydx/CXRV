package com.xiaocydx.sample.paging

import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.staggered
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.paging.config.pagingSwipeRefresh
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle

/**
 * @author xcc
 * @date 2022/2/17
 */
class StaggeredLayoutFragment : PagingFragment() {

    override fun initView() {
        rvPaging
            .staggered(spanCount = 3)
            .fixedSize()
            .divider {
                height = 5.dp
                width = 5.dp
                color = 0xFF9DAA8F.toInt()
                verticalEdge = true
                horizontalEdge = true
            }
            // .paging(fooAdapter) // 无拖拽刷新
            .pagingSwipeRefresh(fooAdapter)
    }

    override fun initCollect() {
        super.initCollect()
        listViewModel.enableMultiTypeFoo()
        listViewModel.flow
            .onEach(fooAdapter.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()
    }
}