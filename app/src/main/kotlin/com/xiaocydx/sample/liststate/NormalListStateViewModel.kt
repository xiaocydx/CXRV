package com.xiaocydx.sample.liststate

import androidx.lifecycle.ViewModel
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.addItem
import com.xiaocydx.cxrv.list.asFlow
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.removeItemAt
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

    fun flow() = state.asFlow()

    fun refresh() {
        val newList = (1..100).map { createFoo(num = it) }
        state.submitList(newList)
    }

    fun insertItem() {
        val item = createFoo(num = state.currentList.size)
        state.addItem(0, item)
    }

    fun deleteItem(position: Int = 0) {
        state.removeItemAt(position)
    }

    fun clearOddItem() {
        state.submitTransform { filter { it.num % 2 == 0 } }
    }

    fun clearEvenItem() {
        state.submitTransform { filter { it.num % 2 != 0 } }
    }

    fun clearAllItem() {
        state.clear()
    }

    private fun createFoo(num: Int, tag: String = javaClass.simpleName): Foo {
        return Foo(id = "$tag-$num", name = "Foo-$num", num, "", FooType.TYPE1)
    }
}