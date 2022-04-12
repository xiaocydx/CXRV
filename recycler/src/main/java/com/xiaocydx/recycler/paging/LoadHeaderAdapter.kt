package com.xiaocydx.recycler.paging

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.hasDisplayItem
import com.xiaocydx.recycler.extension.resolveLayoutParams
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.ListChangedListener
import com.xiaocydx.recycler.concat.ViewAdapter

/**
 * 加载头部适配器
 *
 * @author xcc
 * @date 2021/9/17
 */
internal class LoadHeaderAdapter(
    private val config: LoadHeader.Config,
    private val adapter: ListAdapter<*, *>
) : ViewAdapter<LoadHeaderAdapter.ViewHolder>(),
    LoadStatesListener, ListChangedListener<Any> {
    private var showType: ShowType = ShowType.NONE
    private var loadStates: LoadStates = LoadStates.Incomplete

    init {
        val collector = adapter.pagingCollector
        config.setCollector(collector)
        adapter.addListChangedListener(this)
        collector.addLoadStatesListener(this)
    }

    override fun getItemViewType(): Int = hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val loadHeader = LoadHeader(parent.context, config)
        return ViewHolder(loadHeader).resolveLayoutParams(parent)
    }

    override fun onBindViewHolder(
        holder: ViewHolder
    ) = with(holder.loadHeader) {
        when (showType) {
            ShowType.NONE -> return@with
            ShowType.LOADING -> postShowLoading()
            ShowType.FAILURE -> postShowFailure(
                exception = loadStates.exception
                    ?: throw AssertionError("失败类型的显示出现断言异常")
            )
            ShowType.EMPTY -> postShowEmpty()
        }
    }

    /**
     * 刷新加载完成前和完成时，在加载状态更改时更新视图类型
     *
     * @param previous 之前的加载状态集合
     * @param current  当前的加载状态集合
     */
    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        loadStates = current
        val currentType = getCurrentType(current)
        if (showType != currentType) {
            showType = currentType
            updateLoadHeader()
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
            showType == ShowType.EMPTY && adapter.hasDisplayItem -> {
                showType = ShowType.NONE
                updateLoadHeader()
            }
            showType == ShowType.NONE && !adapter.hasDisplayItem -> {
                showType = ShowType.EMPTY
                updateLoadHeader()
            }
        }
    }

    private fun getCurrentType(current: LoadStates): ShowType = when {
        adapter.hasDisplayItem -> ShowType.NONE
        current.isLoading -> ShowType.LOADING
        current.isFailure -> ShowType.FAILURE
        current.isFully -> ShowType.EMPTY
        else -> showType
    }

    private fun updateLoadHeader() {
        updateItem(show = showType != ShowType.NONE, anim = NeedAnim.NOT_ALL)
    }

    private enum class ShowType {
        NONE, LOADING, FAILURE, EMPTY
    }

    class ViewHolder(val loadHeader: LoadHeader) : RecyclerView.ViewHolder(loadHeader)
}