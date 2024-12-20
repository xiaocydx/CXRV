package com.xiaocydx.sample.paging.local

import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.paging.replaceWithSwipeRefresh
import com.xiaocydx.accompanist.paging.withPaging
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector

/**
 * @author xcc
 * @date 2022/2/17
 */
class LinearPagingFragment : PagingFragment() {

    override fun initView() {
        rvPaging
            .linear()
            .divider(5.dp, 5.dp) {
                edge(Edge.all())
                color(0xFF979EC4.toInt())
            }
            .adapter(fooAdapter.withPaging())
            .replaceWithSwipeRefresh(fooAdapter)
    }

    override fun initCollect() {
        super.initCollect()
        fooViewModel.pagingFlow
            .onEach(fooAdapter.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)
    }
}