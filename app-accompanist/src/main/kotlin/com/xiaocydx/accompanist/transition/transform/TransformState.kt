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

package com.xiaocydx.accompanist.transition.transform

import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.accompanist.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.lang.ref.WeakReference

internal val FragmentActivity.transformState: TransformState
    get() {
        val key = R.id.tag_common_transform_state
        var state = window.decorView.getTag(key) as? TransformState
        if (state == null) {
            state = TransformState()
            window.decorView.setTag(key, state)
        }
        return state
    }

internal class TransformState {
    private var senderRootRef: WeakReference<View>? = null
    private var senderImageRef: WeakReference<ImageView>? = null
    private val _receiverEvent = MutableSharedFlow<ReceiverEvent>(extraBufferCapacity = Int.MAX_VALUE)
    val receiverEvent = _receiverEvent.asSharedFlow()

    fun setSenderViews(root: View?, image: ImageView?) {
        if (senderRootRef?.get() !== root) {
            senderRootRef = root.let(::WeakReference)
        }
        if (senderImageRef?.get() !== image) {
            senderImageRef = image.let(::WeakReference)
        }
    }

    fun emitReceiverEvent(event: ReceiverEvent) {
        _receiverEvent.tryEmit(event)
    }

    fun getSenderRoot(): View? {
        return senderRootRef?.get()
    }

    fun getSenderImage(): ImageView? {
        return senderImageRef?.get()
    }
}