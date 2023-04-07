package com.xiaocydx.cxrv.itemvisible

import com.xiaocydx.cxrv.layout.LayoutManagerExtensions

/**
 * @author xcc
 * @date 2022/11/22
 */
internal class TestLayoutManagerExtensions : LayoutManagerExtensions<TestLayoutManager> {

    override fun findFirstVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findFirstVisibleItemPosition()
    }

    override fun findFirstCompletelyVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findFirstCompletelyVisibleItemPosition()
    }

    override fun findLastVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findLastVisibleItemPosition()
    }

    override fun findLastCompletelyVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findLastCompletelyVisibleItemPosition()
    }
}