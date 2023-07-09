/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("RecycleAllViewsInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Parcelable
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.list.Disposable

/**
 * 将[RecyclerView]的子View和离屏缓存回收进[RecycledViewPool]
 *
 * ### 增加回收上限
 * 当回收数量超过`viewType`的回收上限时，调用[increaseMaxScrap]获取增加后的回收上限，
 * 回收流程会先对`viewType`的回收上限不断`+1`，当达到增加后的回收上限时，则不再`+1`，
 * 这样做的目的是避免后续缓存冗余的[ViewHolder]。
 * ```
 * // viewType增加后的回收上限返回20，如果实际只回收了10个ViewHolder，
 * // 那么回收上限只增加到10，而不是20，避免后续缓存冗余的ViewHolder。
 * recyclerView.recycleAllViews { viewType, currentMaxScrap, initialState -> 20 }
 * ```
 *
 * ### 确保执行回调
 * 该函数确保执行回收流程相关的回调，回调包括不限于：
 * 1. [Adapter.onViewRecycled]
 * 2. [Adapter.onViewDetachedFromWindow]
 * 3. [RecyclerListener.onViewRecycled]
 * 4. [OnChildAttachStateChangeListener.onChildViewDetachedFromWindow]
 *
 * @param increaseMaxScrap 获取`viewType`增加后的回收上限。
 */
fun RecyclerView.recycleAllViews(increaseMaxScrap: IncreaseMaxScrap) {
    RecycleAllViewsRunner(this, increaseMaxScrap).run()
}

/**
 * 当[RecyclerView]从Window分离时，通过[recycleAllViews]将子View和离屏缓存回收进[RecycledViewPool]
 *
 * **注意**：由于[StaggeredGridLayoutManager.onDetachedFromWindow]的清除逻辑，
 * 比[View.OnAttachStateChangeListener.onViewDetachedFromWindow]先执行，
 * 因此`saveState = true`无法保存正确的状态，恢复的滚动位置带有一定程度的偏移。
 * 此时需要调用者自行保存状态，若已设置[StaggeredGridLayoutManagerCompat]，
 * 则不需要自行保存状态，[StaggeredGridLayoutManagerCompat]已兼容这种情况：
 * ```
 * recyclerView.staggered(spanCount) // 设置StaggeredGridLayoutManagerCompat
 * recyclerView.setRecycleAllViewsOnDetach(increaseMaxScrap)
 * ```
 *
 * @param increaseMaxScrap 获取`viewType`增加后的回收上限。
 * @param saveState `true`表示当[RecyclerView]从Window分离时，
 * 保存[LayoutManager]的状态，当再次添加到Window或者重建时，能够恢复滚动位置。
 * @return 调用[Disposable.dispose]可以移除设置的回收处理。
 */
fun RecyclerView.setRecycleAllViewsOnDetach(
    saveState: Boolean = true,
    increaseMaxScrap: IncreaseMaxScrap
): Disposable {
    return RecycleAllViewsOnDetachDisposable().attach(this, saveState, increaseMaxScrap)
}

/**
 * [setRecycleAllViewsOnDetach]的简化函数，适用于增加后的回收上限是固定值的场景
 *
 * @param maxScrap  所有`viewType`增加后的回收上限。
 * @param saveState `true`表示当[RecyclerView]从Window分离时，
 * 保存[LayoutManager]的状态，当再次添加到Window或者重建时，能够恢复滚动位置。
 */
fun RecyclerView.setRecycleAllViewsOnDetach(maxScrap: Int, saveState: Boolean = true): Disposable {
    return setRecycleAllViewsOnDetach(saveState) { _, _, _ -> maxScrap }
}

/**
 * 回收数量超过`viewType`的回收上限，获取`viewType`增加后的回收上限
 */
typealias IncreaseMaxScrap = (viewType: Int, currentMaxScrap: Int, initialState: InitialState) -> Int

/**
 * [RecyclerView]的初始状态，用于[IncreaseMaxScrap]
 */
interface InitialState {
    /**
     * `recyclerView.layoutManager.itemCount`
     */
    val itemCount: Int

    /**
     * `recyclerView.childCount`
     */
    val childCount: Int
}

private class RecycleAllViewsRunner(
    private val rv: RecyclerView,
    private val increaseMaxScrap: IncreaseMaxScrap
) : RecyclerListener, InitialState {
    override var itemCount = 0
    override var childCount = 0

    /**
     * 在[ViewHolder]被回收进[RecycledViewPool]之前，
     * 先执行[onViewRecycled]，尝试增加`viewType`的回收上限。
     */
    fun run() {
        itemCount = rv.layoutManager?.itemCount ?: 0
        childCount = rv.childCount
        val recycler = rv.mRecycler
        rv.addRecyclerListener(this)
        rv.layoutManager?.removeAndRecycleAllViews(recycler)
        recycler.clear()
        rv.removeRecyclerListener(this)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clearReference()
        val viewType = holder.itemViewType
        val scrap = rv.recycledViewPool.mScrap
        val scrapData = scrap[viewType] ?: return

        // 若可以继续回收，则不需要增加回收上限
        var currentMaxScrap = scrapData.mMaxScrap
        val scrapSize = scrapData.mScrapHeap.size
        if (scrapSize < currentMaxScrap) return

        // 预创建ViewHolder的场景可能会出现scrapSize > currentMaxScrap
        currentMaxScrap = currentMaxScrap.coerceAtLeast(scrapSize)
        val expectMaxScrap = increaseMaxScrap(viewType, currentMaxScrap, this)
        if (currentMaxScrap < expectMaxScrap) scrapData.mMaxScrap = currentMaxScrap + 1
    }

    private fun ViewHolder.clearReference() {
        val lp = itemView.layoutParams
        if (lp !is StaggeredGridLayoutManager.LayoutParams) return
        // chain：StaggeredGridLayoutManager.mRecyclerView -> Span
        lp.mSpan = null
    }
}

private class RecycleAllViewsOnDetachDisposable : OnAttachStateChangeListener, Disposable {
    private var saveState = false
    private var rv: RecyclerView? = null
    private var runner: RecycleAllViewsRunner? = null
    private var pendingSavedState: Parcelable? = null
    private var canDisableStaggeredCompat = false
    override val isDisposed: Boolean
        get() = rv == null && runner == null

    fun attach(
        rv: RecyclerView,
        saveState: Boolean,
        increaseMaxScrap: IncreaseMaxScrap
    ): Disposable {
        this.rv = rv
        this.saveState = saveState
        this.runner = RecycleAllViewsRunner(rv, increaseMaxScrap)
        rv.addOnAttachStateChangeListener(this)
        tryEnableStaggeredCompat()
        return this
    }

    override fun onViewAttachedToWindow(view: View) {
        if (pendingSavedState != null) {
            (view as? RecyclerView)?.layoutManager
                ?.onRestoreInstanceState(pendingSavedState)
        }
        pendingSavedState = null
    }

    override fun onViewDetachedFromWindow(view: View) {
        val rv = view as? RecyclerView ?: return
        val lm = rv.layoutManager
        if (lm != null && saveState) {
            pendingSavedState = lm.onSaveInstanceState()
            // 这是一种取巧的做法，对LayoutManager实现类的mPendingSavedState赋值，
            // 确保Fragment销毁时能保存状态，Fragment重建时恢复RecyclerView的滚动位置。
            pendingSavedState?.let(lm::onRestoreInstanceState)
        }
        runner?.run()
    }

    private fun tryEnableStaggeredCompat() {
        val lm = rv?.layoutManager as? StaggeredGridLayoutManagerCompat
        if (saveState && lm != null) {
            saveState = false
            canDisableStaggeredCompat = !lm.isSaveStateOnDetach
            lm.isSaveStateOnDetach = true
        }
    }

    private fun tryDisableStaggeredCompat() {
        val lm = rv?.layoutManager as? StaggeredGridLayoutManagerCompat
        if (canDisableStaggeredCompat && lm != null) {
            lm.isSaveStateOnDetach = false
        }
    }

    override fun dispose() {
        tryDisableStaggeredCompat()
        rv?.removeOnAttachStateChangeListener(this)
        rv = null
        runner = null
        pendingSavedState = null
    }
}