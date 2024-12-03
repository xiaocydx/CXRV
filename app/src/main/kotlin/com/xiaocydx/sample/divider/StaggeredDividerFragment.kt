package com.xiaocydx.sample.divider

import android.graphics.drawable.Drawable
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.concat.addFooter
import com.xiaocydx.cxrv.concat.addHeader
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.removeItem
import com.xiaocydx.cxrv.list.staggered

/**
 * @author xcc
 * @date 2023/10/4
 */
class StaggeredDividerFragment : NormalDividerFragment() {

    /**
     * **注意**：瀑布流布局的分割线，最后一行的判断逻辑存在已知缺陷，
     * 设置[Drawable]（例如颜色），最后一行的绘制结果可能不符合预期。
     */
    override fun initView() {
        enableMultiTypeFoo()
        super.initView()
        rvDivider
            .staggered(spanCount = 3)
            .divider(5.dp, 5.dp) {
                edge(Edge.all())
                color(0xFF979EC4.toInt())
            }
            .adapter(fooAdapter)

        // 扩展函数divider()，默认排除Header和Footer
        rvDivider.addHeader(header)
        rvDivider.addFooter(footer)

        // 瀑布流布局的分割线不会跟随itemView的X/Y偏移而变化
        rvDivider.itemAnimator?.moveDuration = 500
        fooAdapter.doOnItemClick(action = fooAdapter::removeItem)
    }
}