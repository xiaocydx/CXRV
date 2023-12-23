package com.xiaocydx.sample.divider

import androidx.annotation.CallSuper
import com.xiaocydx.cxrv.list.insertItem
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.list.size
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.common.FooListAdapter

/**
 * @author xcc
 * @date 2023/10/4
 */
abstract class NormalDividerFragment : DividerFragment() {
    protected val fooAdapter = FooListAdapter()

    @CallSuper
    override fun initView() {
        super.initView()
        fooAdapter.submitList((1..20).map(::createFoo))
    }

    final override fun insertItem() {
        fooAdapter.insertItem(createFoo(fooAdapter.size + 1))
    }

    final override fun removeItem() {
        fooAdapter.removeItemAt(0)
    }
}