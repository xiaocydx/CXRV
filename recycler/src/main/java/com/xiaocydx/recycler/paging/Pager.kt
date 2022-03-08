package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.extension.flowOnMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/**
 * 分页的主要入口，提供[PagingData]数据流，当调用了[refresh]，
 * 会发射新的[PagingData]并取消旧的[PagingData]的事件流。
 *
 * 1.在Repository下创建[Pager]，对外提供[Pager.flow]
 * ```
 * class FooRepository {
 *     private val pager: Pager<String, Foo> = ...
 *     val flow = pager.flow
 *
 *     fun refresh() {
 *         pager.refresh()
 *     }
 * }
 *
 * 2.对ViewModel注入Repository
 * class FooViewModel(
 *     private val repository: FooRepository
 * ) : ViewModel() {
 *     // 可以对分页事件流做数据变换
 *     val flow = repository.flow
 * }
 *
 * 3.在视图控制器下收集`viewModel.flow`
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          lifecycleScope.launch {
 *              adapter.emitAll(viewModel.flow)
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
) {
    private val refreshEvent = ConflatedEvent<Unit>()
    private var mediator: PagingMediatorImpl<K, T>? = null
    val loadStates: LoadStates
        get() = mediator?.loadStates ?: LoadStates.Incomplete

    val flow: Flow<PagingData<T>> = flow {
        refreshEvent.flow
            .onStart {
                // 触发初始化加载
                emit(Unit)
            }.collect {
                mediator?.close()
                val fetcher: PagingFetcher<K, T> =
                        PagingFetcher(initKey, config, source)
                mediator = PagingMediatorImpl(fetcher, refreshEvent)
                emit(PagingData(fetcher.flow, mediator!!))
            }
    }.conflate().flowOnMain()

    fun refresh() {
        refreshEvent.send(Unit)
    }

    private class PagingMediatorImpl<K : Any, T : Any>(
        private val fetcher: PagingFetcher<K, T>,
        private val refreshEvent: ConflatedEvent<Unit>
    ) : PagingMediator {
        override val loadStates: LoadStates
            get() = fetcher.loadStates

        override fun refresh() {
            refreshEvent.send(Unit)
        }

        override fun append() {
            fetcher.append()
        }

        override fun retry() {
            fetcher.retry()
        }

        fun close() {
            fetcher.close()
        }
    }
}