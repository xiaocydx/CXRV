package com.xiaocydx.sample.divider

import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.concat.addFooter
import com.xiaocydx.cxrv.concat.addHeader
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.removeItem

/**
 * @author xcc
 * @date 2023/10/4
 */
class LinearDividerFragment : NormalDividerFragment() {

    override fun initView() {
        super.initView()
        rvDivider
            .linear()
            .divider(5.dp, 5.dp) {
                edge(Edge.all())
                color(0xFF979EC4.toInt())
            }
            .adapter(fooAdapter)

        // 扩展函数divider()，默认排除Header和Footer
        rvDivider.addHeader(header)
        rvDivider.addFooter(footer)

        // 线性布局的分割线跟随itemView的X/Y偏移和alpha而变化
        rvDivider.itemAnimator?.removeDuration = 500
        fooAdapter.doOnSimpleItemClick(fooAdapter::removeItem)
    }
}