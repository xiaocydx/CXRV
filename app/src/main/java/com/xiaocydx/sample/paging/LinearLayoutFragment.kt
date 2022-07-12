package com.xiaocydx.sample.paging

import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.paging.config.pagingSwipeRefresh
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.launchIn

/**
 * @author xcc
 * @date 2022/2/17
 */
class LinearLayoutFragment : PagingFragment() {

    override fun initView() {
        rvPaging
            .linear()
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
        listViewModel.flow
            .onEach(fooAdapter.pagingCollector)
            .flowWithLifecycle(viewLifecycle)
            .launchIn(viewLifecycleScope)
    }
}