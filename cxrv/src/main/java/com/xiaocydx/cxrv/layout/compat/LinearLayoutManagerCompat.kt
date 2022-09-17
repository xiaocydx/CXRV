@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.ExperimentalFeature
import com.xiaocydx.cxrv.layout.callback.CompositeLayoutManagerCallback

/**
 * 提供兼容属性的[LinearLayoutManager]
 *
 * @author xcc
 * @date 2022/8/11
 */
open class LinearLayoutManagerCompat : LinearLayoutManager {
    private val scrollHelper = ScrollToFirstOnUpdateHelper()
    private val saveStateHelper = SaveInstanceStateOnDetachHelper()
    private val invalidateHelper = InvalidateItemDecorationsOnUpdateHelper()
    private val dispatcher = CompositeLayoutManagerCallback(initialCapacity = 3)

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        @Orientation orientation: Int,
        reverseLayout: Boolean
    ) : super(context, orientation, reverseLayout)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        dispatcher.addLayoutManagerCallback(scrollHelper)
        dispatcher.addLayoutManagerCallback(saveStateHelper)
        dispatcher.addLayoutManagerCallback(invalidateHelper)
    }

    /**
     * 是否启用兼容（默认启用）：
     * 往列表首位插入或交换item时，若当前首位完全可见，则滚动到更新后的首位。
     */
    var isScrollToFirstOnUpdate: Boolean
        get() = scrollHelper.isEnabled
        set(value) {
            scrollHelper.isEnabled = value
        }

    /**
     * 是否启用兼容（默认不启用）：
     * 在[onDetachedFromWindow]时保存[LayoutManager]的状态，
     * 在[onAttachedToWindow]时恢复[LayoutManager]的状态。
     */
    var isSaveStateOnDetach: Boolean
        get() = saveStateHelper.isEnabled
        set(value) {
            saveStateHelper.isEnabled = value
        }

    /**
     * 是否启用兼容（默认启用）：
     * 列表更新时调用[RecyclerView.invalidateItemDecorations]，
     * 解决[ItemDecoration.getItemOffsets]调用不完整，导致实现复杂的问题。
     */
    var isInvalidateItemDecorationsOnUpdate: Boolean
        get() = invalidateHelper.isEnabled
        set(value) {
            invalidateHelper.isEnabled = value
        }

    @CallSuper
    override fun setRecyclerView(recyclerView: RecyclerView?) {
        super.setRecyclerView(recyclerView)
        if (recyclerView == null) dispatcher.onCleared()
    }

    @CallSuper
    override fun onAttachedToWindow(view: RecyclerView) {
        dispatcher.onAttachedToWindow(view)
        super.onAttachedToWindow(view)
    }

    @CallSuper
    override fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        dispatcher.onDetachedFromWindow(view, recycler)
        super.onDetachedFromWindow(view, recycler)
    }

    @CallSuper
    override fun onAdapterChanged(oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        dispatcher.onAdapterChanged(layout = this, oldAdapter, newAdapter)
    }

    @CallSuper
    override fun onLayoutCompleted(state: State) {
        dispatcher.onLayoutCompleted(layout = this, state)
        super.onLayoutCompleted(state)
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun generateLayoutParams(c: Context, attrs: AttributeSet): RecyclerView.LayoutParams {
        return LayoutParams(c, attrs)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
        return if (lp is MarginLayoutParams) LayoutParams(lp) else LayoutParams(lp)
    }

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
        return lp is LayoutParams
    }

    @CallSuper
    override fun layoutDecoratedWithMargins(child: View, left: Int, top: Int, right: Int, bottom: Int) {
        super.layoutDecoratedWithMargins(child, left, top, right, bottom)
        val lp = child.layoutParams as? LayoutParams ?: return
        val rv = lp.mViewHolder?.mOwnerRecyclerView
        lp.maybeIgnoreConsumedInPreLayout = rv?.isPreLayout == true && !lp.isItemRemoved
    }

    @ExperimentalFeature
    internal var hasFixedItemSize = false

    class LayoutParams : RecyclerView.LayoutParams {
        internal var maybeIgnoreConsumedInPreLayout = false

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: MarginLayoutParams) : super(source)
        constructor(source: ViewGroup.LayoutParams) : super(source)
        constructor(source: RecyclerView.LayoutParams) : super(source)

        override fun isItemChanged(): Boolean {
            var isItemChanged = super.isItemChanged()
            if (isItemChanged && maybeIgnoreConsumedInPreLayout) {
                val rv = mViewHolder?.mOwnerRecyclerView
                val lm = rv?.layoutManager as? LinearLayoutManagerCompat
                if (rv?.isPreLayout == true && lm?.hasFixedItemSize == true) isItemChanged = false
            }
            maybeIgnoreConsumedInPreLayout = false
            return isItemChanged
        }
    }
}