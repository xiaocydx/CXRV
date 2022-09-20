@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.LayoutManager

@PublishedApi
internal var LayoutManager.isBoundCheckCompatEnabled: Boolean
    get() = isHorizontalBoundCheckCompatEnabled && isVerticalBoundCheckCompatEnabled
    set(isEnabled) {
        isHorizontalBoundCheckCompatEnabled = isEnabled
        isVerticalBoundCheckCompatEnabled = isEnabled
    }

private var LayoutManager.isHorizontalBoundCheckCompatEnabled: Boolean
    get() = mHorizontalBoundCheck?.mCallback is HorizontalBoundCheckCallbackCompat
    set(isEnabled) {
        val boundsCheck = mHorizontalBoundCheck ?: return
        val callback = boundsCheck.mCallback
        val newBoundsCheck = if (isEnabled) {
            if (callback is HorizontalBoundCheckCallbackCompat) return
            ViewBoundsCheck(HorizontalBoundCheckCallbackCompat(this, callback))
        } else {
            if (callback !is HorizontalBoundCheckCallbackCompat) return
            ViewBoundsCheck(callback.delegate)
        }
        newBoundsCheck.mBoundFlags = boundsCheck.mBoundFlags
        mHorizontalBoundCheck = newBoundsCheck
    }

private var LayoutManager.isVerticalBoundCheckCompatEnabled: Boolean
    get() = mVerticalBoundCheck?.mCallback is VerticalBoundCheckCallbackCompat
    set(isEnabled) {
        val boundsCheck = mVerticalBoundCheck ?: return
        val callback = boundsCheck.mCallback
        val newBoundsCheck = if (isEnabled) {
            if (callback is VerticalBoundCheckCallbackCompat) return
            ViewBoundsCheck(VerticalBoundCheckCallbackCompat(this, callback))
        } else {
            if (callback !is VerticalBoundCheckCallbackCompat) return
            ViewBoundsCheck(callback.delegate)
        }
        newBoundsCheck.mBoundFlags = boundsCheck.mBoundFlags
        mVerticalBoundCheck = newBoundsCheck
    }

/**
 * [delegate]是[LayoutManager.mHorizontalBoundCheckCallback]
 */
private class HorizontalBoundCheckCallbackCompat(
    private val layoutManager: LayoutManager,
    val delegate: ViewBoundsCheck.Callback
) : ViewBoundsCheck.Callback by delegate {

    override fun getParentStart(): Int = layoutManager.run {
        if (clipToPadding) paddingLeft else 0
    }

    override fun getParentEnd(): Int = layoutManager.run {
        if (clipToPadding) width - paddingRight else width
    }
}

/**
 * [delegate]是[LayoutManager.mVerticalBoundCheckCallback]
 */
private class VerticalBoundCheckCallbackCompat(
    private val layoutManager: LayoutManager,
    val delegate: ViewBoundsCheck.Callback
) : ViewBoundsCheck.Callback by delegate {

    override fun getParentStart(): Int = layoutManager.run {
        if (clipToPadding) paddingTop else 0
    }

    override fun getParentEnd(): Int = layoutManager.run {
        if (clipToPadding) height - paddingBottom else height
    }
}