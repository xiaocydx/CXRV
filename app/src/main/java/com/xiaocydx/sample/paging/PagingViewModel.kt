package com.xiaocydx.sample.paging

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.recycler.list.ListOwner
import com.xiaocydx.recycler.list.ListState
import com.xiaocydx.recycler.list.addItem
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.paging.*

/**
 * 视图控制器 + [PagingViewModel]作为UI层
 *
 * 视图控制器可以在非活跃状态/重建期间取消收集[flow]，
 * 在恢复活跃状态/重建后，重新收集分页[flow]，更新/恢复视图。
 *
 * @author xcc
 * @date 2022/2/17
 */
class PagingViewModel(
    /**
     * [FooRepository]作为Data层
     */
    private val repository: FooRepository
) : ViewModel() {
    /**
     * 分页的主要入口，提供[PagingData]数据流，当调用了[Pager.refresh]，
     * 会发射新的[PagingData]并取消旧的[PagingData]的事件流。
     */
    private val pager = repository
        .getFooPager(initKey = 1, config = PagingConfig(pageSize = 10))

    /**
     * 列表状态
     *
     * 保存列表数据，跟视图控制器建立基于[ListOwner]的双向通信。
     */
    private val listState = ListState<Foo>()

    /**
     * 分页数据流
     *
     * 1.[transformEventFlow]的转换逻辑可以抽取到业务层中。
     * 2.[storeIn]将转换后的分页数据流和[listState]结合，得到新的分页数据流。
     * 3.[storeIn]传入[viewModelScope]，表示要将分页数据流转换为热流，
     * 在视图控制器处于非活跃状态/重建期间，上游冷流仍然可以发射数据，
     * 在视图控制器恢复活跃状态/重建后，重新收集转换后的热流，完成更新/恢复视图。
     */
    val flow = pager.flow
        .transformEventFlow { flow ->
            flow.transformItem { loadType, item ->
                val suffix = when (loadType) {
                    LoadType.REFRESH -> "Refresh"
                    LoadType.APPEND -> "Append"
                }
                item.copy(name = "${item.name} $suffix")
            }
        }
        .storeIn(listState, viewModelScope)

    /**
     * 保存视图id，视图控制器重建后恢复滚动位置
     */
    val rvId = ViewCompat.generateViewId()

    fun refresh() {
        pager.refresh()
    }

    fun insertItem() {
        val item = createFoo(
            tag = "Pager",
            num = listState.currentList.size
        )
        listState.addItem(0, item)
    }

    fun deleteItem() {
        listState.removeItemAt(0)
    }

    fun createFoo(num: Int, tag: String): Foo {
        return repository.createFoo(num, tag)
    }

    fun enableMultiTypeFoo() {
        repository.enableMultiTypeFoo()
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass === PagingViewModel::class.java) {
                val repository = FooRepository(FooSource(
                    maxKey = 5,
                    resultType = ResultType.Normal
                ))
                return PagingViewModel(repository) as T
            }
            throw IllegalArgumentException()
        }
    }
}