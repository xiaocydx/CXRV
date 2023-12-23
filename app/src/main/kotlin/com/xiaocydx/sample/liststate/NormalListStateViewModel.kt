package com.xiaocydx.sample.liststate

import androidx.lifecycle.ViewModel
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.asStateFlow
import com.xiaocydx.sample.common.Foo
import com.xiaocydx.sample.common.FooType

/**
 * @author xcc
 * @date 2023/8/17
 */
class NormalListStateViewModel : ViewModel() {
    /**
     * [ListState]降级为内部实现，[MutableStateList]替代[ListState]
     */
    private val list = MutableStateList<Foo>()

    init {
        refresh()
    }

    val flow = list.asStateFlow()

    fun refresh() {
        list.submit((1..1000).map(::createFoo))
    }

    fun insertItem() {
        list.add(0, createFoo(list.size + 1))
    }

    fun removeItem(position: Int = 0) {
        list.firstOrNull { it.num == 1 }?.let(list::remove)
    }

    fun clearOdd() {
        list.filter { it.num % 2 == 0 }.let(list::submit)
    }

    fun clearEven() {
        list.filter { it.num % 2 != 0 }.let(list::submit)
    }

    fun clearAll() {
        list.clear()
    }

    private fun createFoo(num: Int): Foo {
        val tag = javaClass.simpleName
        return Foo(id = "$tag-$num", name = "Foo-$num", num, "", FooType.TYPE1)
    }
}