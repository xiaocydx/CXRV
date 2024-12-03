package com.xiaocydx.sample.divider

import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.addDividerItemDecoration
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.insertItem
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.removeItem
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.list.size
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.common.FooListAdapter

/**
 * @author xcc
 * @date 2023/10/4
 */
class ConcatDividerFragment : DividerFragment() {
    private val fooAdapter1 = FooListAdapter()
    private val fooAdapter2 = FooListAdapter()

    /**
     * 除了常规的[divider]，还可以调用[addDividerItemDecoration]匹配`adapter`绘制分割线，
     * 对Header和Footer单独配置分割线的需求并不常见，通常是对多个内容`adapter`配置分割线。
     */
    override fun initView() {
        super.initView()
        val headerAdapter = header.toAdapter()
        val footerAdapter = footer.toAdapter()
        fooAdapter1.submitList((1..5).map(::createFoo))
        fooAdapter2.submitList((1..5).map(::createFoo))
        rvDivider.linear().adapter(
            Concat.header(headerAdapter).footer(footerAdapter)
                .content(fooAdapter1).content(fooAdapter2).concat()
        )

        // 扩展函数addDividerItemDecoration()，匹配adapter绘制分割线
        rvDivider.apply {
            addDividerItemDecoration(fooAdapter1) {
                size(5.dp).edge(Edge.all()).color(0xFF979EC4.toInt())
            }
            addDividerItemDecoration(fooAdapter2) {
                size(5.dp).edge(Edge.all()).color(0xFFD77F7A.toInt())
            }
            addDividerItemDecoration(headerAdapter) { size(5.dp).edge(Edge.all()) }
            addDividerItemDecoration(footerAdapter) { size(5.dp).edge(Edge.all()) }
        }

        // 线性布局的分割线跟随itemView的X/Y偏移和alpha而变化
        rvDivider.itemAnimator?.removeDuration = 500
        fooAdapter1.doOnItemClick { fooAdapter1.removeItem(it) }
        fooAdapter2.doOnItemClick { fooAdapter2.removeItem(it) }
    }

    override fun insertItem() {
        fooAdapter1.insertItem(createFoo(fooAdapter1.size + 1))
        fooAdapter2.insertItem(createFoo(fooAdapter2.size + 1))
    }

    override fun removeItem() {
        fooAdapter1.removeItemAt(0)
        fooAdapter2.removeItemAt(0)
    }
}