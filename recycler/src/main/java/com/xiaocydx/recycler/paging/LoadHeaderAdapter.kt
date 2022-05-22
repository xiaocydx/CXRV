package com.xiaocydx.recycler.paging

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.ViewHolder
import com.xiaocydx.recycler.concat.ViewAdapter
import com.xiaocydx.recycler.extension.hasDisplayItem
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.ListChangedListener

/**
 * 加载头部适配器
 *
 * @author xcc
 * @date 2021/9/17
 */
internal class LoadHeaderAdapter(
    private val config: LoadHeaderConfig,
    private val adapter: ListAdapter<*, *>
) : ViewAdapter<ViewHolder>(), LoadStatesListener, ListChangedListener<Any> {
    private var visible: Visible = Visible.NONE
    private var loadStates: LoadStates = LoadStates.Incomplete

    init {
        val collector = adapter.pagingCollector
        config.complete(
            retry = collector::retry,
            exception = { collector.loadStates.exception }
        )
        adapter.addListChangedListener(this)
        collector.addLoadStatesListener(this)
    }

    override fun getItemViewType(): Int = hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LoadViewLayout(
            context = parent.context,
            loadingItem = config.loadingScope?.getViewItem(),
            successItem = config.emptyScope?.getViewItem(),
            failureItem = config.failureScope?.getViewItem()
        )
        return ViewHolder(itemView, parent).apply {
            itemView.layoutParams.width = config.width
            itemView.layoutParams.height = config.height
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder
    ): Unit = with(holder.itemView as LoadViewLayout) {
        when (visible) {
            Visible.LOADING -> loadingVisible()
            Visible.FAILURE -> failureVisible()
            Visible.EMPTY -> successVisible()
            Visible.NONE -> throw AssertionError("局部刷新出现断言异常")
        }
    }

    /**
     * 加载完毕后，在列表更改时更新空视图的显示情况
     *
     * **注意**：[onListChanged]在[onLoadStatesChanged]之前被调用。
     */
    override fun onListChanged(current: List<Any>) {
        when {
            !loadStates.isFully -> return
            visible == Visible.EMPTY && adapter.hasDisplayItem -> {
                // 此时EMPTY视图已显示，并且列表不为空
                updateLoadHeader(Visible.NONE)
            }
            visible == Visible.NONE && !adapter.hasDisplayItem -> {
                // 此时EMPTY视图未显示，并且列表为空
                updateLoadHeader(Visible.EMPTY)
            }
        }
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        loadStates = current
        updateLoadHeader(current.toVisible())
    }

    private fun LoadStates.toVisible(): Visible = when {
        adapter.hasDisplayItem -> Visible.NONE
        this.isLoading -> if (config.loadingScope != null) Visible.LOADING else Visible.NONE
        this.isFailure -> if (config.failureScope != null) Visible.FAILURE else Visible.NONE
        this.isFully -> if (config.emptyScope != null) Visible.EMPTY else Visible.NONE
        else -> visible
    }

    private fun updateLoadHeader(visible: Visible) {
        if (this.visible != visible) {
            this.visible = visible
            updateItem(show = visible != Visible.NONE, anim = NeedAnim.NOT_ALL)
        }
    }

    private enum class Visible {
        NONE, LOADING, FAILURE, EMPTY
    }
}