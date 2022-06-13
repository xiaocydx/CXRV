@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.LayoutManager

@PublishedApi
internal fun LayoutManager.enableUnsafeViewBoundCheckCompat() {
    if (horizontalBoundCheck.mCallback !is HorizontalBoundCheckCallbackCompat) {
        horizontalBoundCheck = ViewBoundsCheck(HorizontalBoundCheckCallbackCompat(
            layoutManager = this,
            delegate = horizontalBoundCheck.mCallback
        ))
    }

    if (verticalBoundCheck.mCallback !is VerticalBoundCheckCallbackCompat) {
        verticalBoundCheck = ViewBoundsCheck(VerticalBoundCheckCallbackCompat(
            layoutManager = this,
            delegate = verticalBoundCheck.mCallback
        ))
    }
}

private var LayoutManager.horizontalBoundCheck: ViewBoundsCheck
    get() = mHorizontalBoundCheck
    set(value) {
        mHorizontalBoundCheck = value
    }

private var LayoutManager.verticalBoundCheck: ViewBoundsCheck
    get() = mVerticalBoundCheck
    set(value) {
        mVerticalBoundCheck = value
    }

/**
 * [delegate]是[LayoutManager.mHorizontalBoundCheckCallback]
 */
private class HorizontalBoundCheckCallbackCompat(
    private val layoutManager: LayoutManager,
    private val delegate: ViewBoundsCheck.Callback
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
    private val delegate: ViewBoundsCheck.Callback
) : ViewBoundsCheck.Callback by delegate {

    override fun getParentStart(): Int = layoutManager.run {
        if (clipToPadding) paddingTop else 0
    }

    override fun getParentEnd(): Int = layoutManager.run {
        if (clipToPadding) height - paddingBottom else height
    }
}