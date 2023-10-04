package com.xiaocydx.sample.foo

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.insertItem
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.list.size
import com.xiaocydx.cxrv.paging.LoadType
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.cxrv.paging.itemMap
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.flow.onStart

/**
 * 视图控制器 + [FooListViewModel]作为UI层
 *
 * 视图控制器可以在非活跃/重建期间取消收集[flow]，
 * 在恢复活跃/重建后，重新收集分页[flow]，更新/恢复视图。
 *
 * @author xcc
 * @date 2022/2/17
 */
class FooListViewModel(
    /**
     * [FooRepository]作为Data层
     */
    private val repository: FooRepository = FooRepository(
        FooSource(maxKey = 5, resultType = ResultType.Normal)
    )
) : ViewModel() {
    /**
     * 分页的主要入口，提供[PagingData]数据流，当调用了[Pager.refresh]，
     * 会发射新的[PagingData]并取消旧的[PagingData]的事件流。
     */
    private val pager = repository.getFooPager(
        initKey = 1,
        config = PagingConfig(pageSize = 10)
    )

    /**
     * 列表状态
     *
     * 保存列表数据，跟视图控制器建立基于[ListOwner]的双向通信。
     */
    private val state = ListState<Foo>()

    /**
     * 保存视图id，视图控制器重建后恢复滚动位置
     */
    val rvId = ViewCompat.generateViewId()

    /**
     * 是否加载过，可用于视图控制器的重建恢复判断
     */
    var isLoaded: Boolean = false
        private set

    /**
     * 分页数据流
     *
     * 1. [flowMap]的转换逻辑可以抽取到业务层中。
     * 2. [storeIn]将转换后的`Flow<PagingData<Foo>>`和[state]结合。
     * 3. [storeIn]传入[viewModelScope]，表示要将分页数据流转换为热流，
     * 在视图控制器处于非活跃/重建期间，上游冷流仍然可以发射数据，
     * 在视图控制器恢复活跃/重建后，重新收集转换后的热流，完成更新/恢复视图。
     */
    val flow = pager.flow
        .onStart { isLoaded = true }
        .flowMap { flow ->
            flow.itemMap { loadType, item ->
                val suffix = when (loadType) {
                    LoadType.REFRESH -> "Refresh"
                    LoadType.APPEND -> "Append"
                }
                item.copy(name = "${item.name} $suffix")
            }
        }
        .storeIn(state, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun insertItem() {
        state.insertItem(createFoo(state.size + 1))
    }

    fun removeItem() {
        state.removeItemAt(0)
    }

    fun clearAll() {
        state.clear()
    }

    fun createFoo(num: Int): Foo {
        val tag: String = javaClass.simpleName
        return repository.createFoo(num, tag)
    }

    fun enableMultiTypeFoo() {
        repository.enableMultiTypeFoo()
    }
}