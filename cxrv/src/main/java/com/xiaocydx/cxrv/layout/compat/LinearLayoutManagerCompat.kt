@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.layout.callback.CompositeLayoutManagerCallback

/**
 * @author xcc
 * @date 2022/8/11
 */
open class LinearLayoutManagerCompat : LinearLayoutManager {
    private var scrollHelper = ScrollToPositionOnUpdateHelper()
    private val saveStateHelper = SaveInstanceStateOnDetachHelper()
    private var invalidateHelper = InvalidateItemDecorationsHelper()
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

    var isSaveStateOnDetach: Boolean
        get() = saveStateHelper.isEnabled
        set(value) {
            saveStateHelper.isEnabled = value
        }

    init {
        dispatcher.addLayoutManagerCallback(scrollHelper)
        dispatcher.addLayoutManagerCallback(saveStateHelper)
        dispatcher.addLayoutManagerCallback(invalidateHelper)
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