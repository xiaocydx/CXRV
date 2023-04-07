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

@file:JvmName("_LayoutManagerInternalKt")
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