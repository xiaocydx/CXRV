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

package com.xiaocydx.sample.paging.complex.transform

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.transition.Transition
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.sample.R
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent

/**
 * @author xcc
 * @date 2023/8/1
 */
internal class TransformRootView(context: Context) : FrameLayout(context) {
    private var senderView: View? = null

    init {
        layoutParams(matchParent, matchParent)
        id = R.id.transform_root_view_fragment_container
    }

    fun setSenderView(view: View?) {
        senderView = view
    }

    fun createTransition(fragment: Fragment, transform: MaterialContainerTransform): Transition {
        return TransformTransition(fragment, ::senderView, transform)
    }
}