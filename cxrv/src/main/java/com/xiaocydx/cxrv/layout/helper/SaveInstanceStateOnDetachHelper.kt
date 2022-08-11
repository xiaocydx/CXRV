@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * @author xcc
 * @date 2022/8/11
 */
internal class SaveInstanceStateOnDetachHelper : LayoutManagerCallback {
    private var pendingSavedState: Parcelable? = null

    var isEnabled = false
        set(value) {
            field = value
            if (!value) pendingSavedState = null
        }

    override fun onAttachedToWindow(view: RecyclerView) {
        pendingSavedState?.let { view.layoutManager?.onRestoreInstanceState(it) }
        pendingSavedState = null
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        val lm = view.layoutManager
        pendingSavedState = if (isEnabled) lm?.onSaveInstanceState() else null
        // 这是一种取巧的做法，对LayoutManager实现类的mPendingSavedState赋值，
        // 确保Fragment销毁时能保存状态，Fragment重建时恢复RecyclerView的滚动位置。
        pendingSavedState?.let { lm?.onRestoreInstanceState(it) }
    }
}