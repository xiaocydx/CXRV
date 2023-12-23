package com.xiaocydx.sample.paging.local

import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.staggered
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.accompanist.paging.replaceWithSwipeRefresh
import com.xiaocydx.accompanist.paging.withPaging

/**
 * @author xcc
 * @date 2022/2/17
 */
class StaggeredPagingFragment : PagingFragment() {

    override fun initView() {
        rvPaging
            .staggered(spanCount = 3)
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
        fooViewModel.enableMultiTypeFoo()
        fooViewModel.flow
            .onEach(fooAdapter.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)
    }
}