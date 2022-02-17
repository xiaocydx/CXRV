package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.extension.flowOnMain
import com.xiaocydx.recycler.list.ListOwner
import com.xiaocydx.recycler.list.UpdateOp
import kotlinx.coroutines.flow.*

/**
 * 分页的主要入口，提供[PagingData]数据流和[ListOwner]
 *
 * 当调用了[refresh]，会发射新的[PagingData]并取消旧的[PagingData]的事件流。
 * ```
 * // 在ViewModel下创建`pager`，对外提供`flow`
 * class FooViewModel : ViewModel() {
 *     private val pager: Pager<String, Foo> = ...
 *     val flow = pager.flow
 *     // 或者保持收集事件流
 *     val flow = pager.flow.cacheIn(viewModelScope)
 * }
 *
 * // 在视图控制器下收集`viewModel.flow`
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          lifecycleScope.launch {
 *              adapter.emitAll(viewModel.flow)
 *          }
 *          // 或者仅在视图控制器活跃期间内收集`viewModel.flow`
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  adapter.emitAll(viewModel.flow)
 *              }
 *          }
 *     }
 * }
 * ```
 * @author xcc
 * @date 2021/9/13
 */
class Pager<K : Any, T : Any>(
    private val initKey: K,
    private val config: PagingConfig,
    private val source: PagingSource<K, T>
) : ListOwner<T> {
    private val refreshEvent = ConflatedEvent<Unit>()
    private var mediator: PagingFetcherMediator? = null
    override val currentList: List<T>
        get() = mediator?.currentList ?: emptyList()
    val loadStates: LoadStates
        get() = mediator?.loadStates ?: LoadStates.Incomplete

    val flow: Flow<PagingData<T>> = flow {
        refreshEvent.flow
            .onStart {
                // 触发初始化加载
                emit(Unit)
            }.collect {
                mediator?.close()
                mediator = when (mediator) {
                    null -> PagingFetcherMediator()
                    else -> mediator!!.snapshot()
                }
                emit(PagingData(mediator!!.flow, mediator!!))
            }
    }.conflate().flowOnMain()

    fun refresh() {
        refreshEvent.send(Unit)
    }

    override fun updateList(op: UpdateOp<T>) {
        mediator?.updateList(op, dispatch = true)
    }

    private inner class PagingFetcherMediator(
        initVersion: Int = 0,
        initList: List<T>? = null
    ) : PagingMediator<T> {
        private val fetcher: PagingFetcher<K, T> =
                PagingFetcher(initKey, config, source, initList)
        override var updateVersion = initVersion
            private set
        override val loadStates: LoadStates
            get() = fetcher.loadStates
        override val currentList: List<T>
            get() = fetcher.currentList

        val flow: Flow<PagingEvent<T>> = fetcher.flow.onEach {
            if (it is PagingEvent.ListUpdate) {
                updateVersion++
            }
        }.flowOnMain()

        fun snapshot(): PagingFetcherMediator {
            // 将旧提取器的列表，作为新提取器的初始化列表，
            // 确保刷新加载期间，可以正常修改新提取器的列表。
            return PagingFetcherMediator(updateVersion, currentList)
        }

        override fun refresh() {
            this@Pager.refresh()
        }

        override fun append() {
            fetcher.append()
        }

        override fun retry() {
            fetcher.retry()
        }

        override fun updateList(op: UpdateOp<T>) {
            updateList(op, dispatch = false)
        }

        fun updateList(op: UpdateOp<T>, dispatch: Boolean) {
            fetcher.updateList(op, dispatch)
        }

        fun close() {
            fetcher.close()
        }
    }
}