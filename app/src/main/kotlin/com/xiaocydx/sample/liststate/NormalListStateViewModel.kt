package com.xiaocydx.sample.liststate

import androidx.lifecycle.ViewModel
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.asFlow
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.insertItem
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.list.size
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.list.submitTransform
import com.xiaocydx.sample.foo.Foo
import com.xiaocydx.sample.foo.FooType

/**
 * @author xcc
 * @date 2023/8/17
 */
class NormalListStateViewModel : ViewModel() {
    private val state = ListState<Foo>()

    init {
        refresh()
    }

    val flow = state.asFlow()

    fun refresh() {
        state.submitList((1..100).map(::createFoo))
    }

    fun insertItem() {
        state.insertItem(createFoo(state.size + 1))
    }

    fun removeItem(position: Int = 0) {
        state.removeItemAt(position)
    }

    fun clearOdd() {
        state.submitTransform { filter { it.num % 2 == 0 } }
    }

    fun clearEven() {
        state.submitTransform { filter { it.num % 2 != 0 } }
    }

    fun clearAll() {
        state.clear()
    }

    private fun createFoo(num: Int): Foo {
        val tag = javaClass.simpleName
        return Foo(id = "$tag-$num", name = "Foo-$num", num, "", FooType.TYPE1)
    }
}