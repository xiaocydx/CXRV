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

@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * 在[onPreDetachedFromWindow]时保存[LayoutManager]的状态，
 * 在[onPreAttachedToWindow]时恢复[LayoutManager]的状态。
 *
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

    override fun onPreAttachedToWindow(view: RecyclerView) {
        pendingSavedState?.let { view.layoutManager?.onRestoreInstanceState(it) }
        pendingSavedState = null
    }

    override fun onPreDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        val lm = view.layoutManager
        pendingSavedState = if (isEnabled) lm?.onSaveInstanceState() else null
        // 这是一种取巧的做法，对LayoutManager实现类的mPendingSavedState赋值，
        // 确保Fragment销毁时能保存状态，Fragment重建时恢复RecyclerView的滚动位置。
        pendingSavedState?.let { lm?.onRestoreInstanceState(it) }
        super.onPreDetachedFromWindow(view, recycler)
    }
}