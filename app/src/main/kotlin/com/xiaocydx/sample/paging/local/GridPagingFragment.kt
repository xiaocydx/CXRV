package com.xiaocydx.sample.paging.local

import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.launchRepeatOnLifecycle
import com.xiaocydx.sample.paging.config.replaceWithSwipeRefresh
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.viewLifecycle

/**
 * @author xcc
 * @date 2022/2/17
 */
class GridPagingFragment : PagingFragment() {

    override fun initView() {
        rvPaging
            .grid(spanCount = 3)
            .fixedSize()
            .divider(5.dp, 5.dp) {
                edge(Edge.all())
                color(0xFF979EC4.toInt())
            }
            .adapter(fooAdapter.withPaging())
            .replaceWithSwipeRefresh(fooAdapter)
    }

    override fun initCollect() {
        super.initCollect()
        fooViewModel.flow
            .onEach(fooAdapter.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)
    }
}