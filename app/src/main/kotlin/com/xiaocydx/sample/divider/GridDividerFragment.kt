package com.xiaocydx.sample.divider

import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.concat.addFooter
import com.xiaocydx.cxrv.concat.addHeader
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.removeItem

/**
 * @author xcc
 * @date 2023/10/4
 */
class GridDividerFragment : NormalDividerFragment() {

    override fun initView() {
        super.initView()
        rvDivider
            .grid(spanCount = 3)
            .fixedSize()
            .divider(5.dp, 5.dp) {
                edge(Edge.all())
                color(0xFF979EC4.toInt())
            }
            .adapter(fooAdapter)

        // 扩展函数divider()，默认排除Header和Footer
        rvDivider.addHeader(header)
        rvDivider.addFooter(footer)

        // 网格布局的分割线不会跟随itemView的X/Y偏移而变化
        rvDivider.itemAnimator?.moveDuration = 500
        fooAdapter.doOnSimpleItemClick(fooAdapter::removeItem)
    }
}