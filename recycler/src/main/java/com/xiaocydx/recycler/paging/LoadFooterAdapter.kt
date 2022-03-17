package com.xiaocydx.recycler.paging

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.hasDisplayItem
import com.xiaocydx.recycler.extension.resolveLayoutParams
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.ListChangedListener
import com.xiaocydx.recycler.widget.ViewAdapter

/**
 * 加载尾部适配器
 *
 * @author xcc
 * @date 2021/9/17
 */
internal class LoadFooterAdapter(
    private val config: LoadFooter.Config,
    private val adapter: ListAdapter<*, *>
) : ViewAdapter<LoadFooterAdapter.ViewHolder>(),
    LoadStatesListener, ListChangedListener<Any> {
    private var showType: ShowType = ShowType.NONE
    private var loadStates: LoadStates = LoadStates.Incomplete
    private var previousNotEmpty = adapter.hasDisplayItem
    private val RecyclerView.supportsAddAnimations: Boolean
        get() = (itemAnimator?.addDuration ?: -1) > 0

    init {
        val collector = adapter.pagingCollector
        config.setCollector(collector)
        adapter.addListChangedListener(this)
        collector.addLoadStatesListener(this)
    }

    override fun getItemViewType(): Int = hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val loadFooter = LoadFooter(parent.context, config)
        return ViewHolder(loadFooter).resolveLayoutParams(parent)
    }

    override fun onBindViewHolder(
        holder: ViewHolder
    ) = with(holder.loadFooter) {
        when (showType) {
            ShowType.NONE -> return@with
            ShowType.LOADING -> postShowLoading()
            ShowType.FAILURE -> postShowFailure(
                exception = loadStates.exception
                    ?: throw AssertionError("失败类型的显示出现断言异常")
            )
            ShowType.FULLY -> recyclerView?.let(::tryPostShowFully)
        }
    }

    /**
     * 末尾加载完成前和完成时，在加载状态更改时更新视图类型
     *
     * @param previous 之前的加载状态集合
     * @param current  当前的加载状态集合
     */
    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        loadStates = current
        val currentType = getCurrentType(current)
        if (showType != currentType) {
            showType = currentType
            if (previousNotEmpty
                    && recyclerView?.supportsAddAnimations == true
                    && previous.appendToFully(current)) {
                // 末尾加载完全时，会在loadFooter前面插入item，
                // 此时若有item动画，则loadFooter看起来像是被“挤下去”，体验并不好，
                // 因此先移除loadFooter，再添加回loadFooter，将视图类型更新为加载完全。
                hideLoadFooter()
                recyclerView?.post { showLoadFooter() }
            } else {
                updateLoadFooter()
            }
        }
        previousNotEmpty = adapter.hasDisplayItem
    }

    /**
     * 加载完全后，在列表更改时更新加载完全视图的显示情况
     *
     * **注意**：[onListChanged]在[onLoadStatesChanged]之前被调用。
     */
    override fun onListChanged(current: List<Any>) {
        val currentNotEmpty = adapter.hasDisplayItem
        when {
            !loadStates.isFully -> return
            showType == ShowType.FULLY && currentNotEmpty -> {
                updateLoadFooter()
            }
            showType == ShowType.FULLY && !currentNotEmpty -> {
                showType = ShowType.NONE
                updateLoadFooter()
            }
            showType == ShowType.NONE && currentNotEmpty -> {
                showType = ShowType.FULLY
                updateLoadFooter()
            }
        }
        previousNotEmpty = currentNotEmpty
    }

    private fun getCurrentType(current: LoadStates): ShowType {
        val append = current.append
        return when {
            !adapter.hasDisplayItem -> ShowType.NONE
            current.isFully -> ShowType.FULLY
            append.isIncomplete -> ShowType.NONE
            append.isLoading -> ShowType.LOADING
            append.isFailure -> ShowType.FAILURE
            append.isSuccess -> ShowType.NONE
            else -> showType
        }
    }

    private fun updateLoadFooter() {
        updateItem(show = showType != ShowType.NONE, anim = NeedAnim.NOT_ALL)
    }

    private fun showLoadFooter() {
        updateItem(show = true, anim = NeedAnim.NOT_ALL)
    }

    private fun hideLoadFooter() {
        updateItem(show = false, anim = NeedAnim.NOT_ALL)
    }

    private enum class ShowType {
        NONE, LOADING, FAILURE, FULLY
    }

    class ViewHolder(val loadFooter: LoadFooter) : RecyclerView.ViewHolder(loadFooter)
}