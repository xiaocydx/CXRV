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

package com.xiaocydx.sample.viewpager2.nested

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * 将此类作为[RecyclerView]等滚动控件的容器，
 * 可处理[ViewPager2]嵌套滚动控件的滚动冲突。
 *
 * 此类不支持处理多指的滚动冲突，这会增加代码的复杂度，
 * 而且调用场景也不需要处理多指的滚动冲突，因为实用性较低。
 *
 * @author xcc
 * @date 2022/7/8
 */
class Vp2NestedScrollableHost @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val handler = Vp2NestedScrollableHandler(host = this)

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        getChildAt(0)?.let { child ->
            handler.handleInterceptTouchEvent(child, e)
        }
        return super.onInterceptTouchEvent(e)
    }
}