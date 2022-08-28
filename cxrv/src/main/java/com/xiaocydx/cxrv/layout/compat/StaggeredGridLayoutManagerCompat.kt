@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.layout.callback.CompositeLayoutManagerCallback

/**
 * 提供兼容属性的[StaggeredGridLayoutManager]
 *
 * @author xcc
 * @date 2022/8/11
 */
open class StaggeredGridLayoutManagerCompat : StaggeredGridLayoutManager {
    private val scrollHelper = ScrollToFirstOnUpdateHelper()
    private val saveStateHelper = SaveInstanceStateOnDetachHelper()
    private val invalidateHelper = InvalidateItemDecorationsOnUpdateHelper()
    private val dispatcher = CompositeLayoutManagerCallback(initialCapacity = 3)

    constructor(
        spanCount: Int,
        @RecyclerView.Orientation orientation: Int
    ) : super(spanCount, orientation)

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
     * 是否启用兼用（默认启用）：
     * 往列表首位插入或交换item时，若当前首位完全可见，则滚动到更新后的首位。
     */
    var isScrollToFirstOnUpdate: Boolean
        get() = scrollHelper.isEnabled
        set(value) {
            scrollHelper.isEnabled = value
        }

    /**
     * 是否启用兼用（默认不启用）：
     * 在[onDetachedFromWindow]时保存[LayoutManager]的状态，
     * 在[onAttachedToWindow]时恢复[LayoutManager]的状态。
     *
     * 由于`super.onDetachedFromWindow(view, recycler)`会执行清除逻辑，
     * 因此需要在其之前保存状态，确保`ViewPager2`嵌套[RecyclerView]这些场景，
     * [RecyclerView]的滚动位置能够被正确恢复。
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
}