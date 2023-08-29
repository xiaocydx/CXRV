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

package com.xiaocydx.cxrv.viewpager2.nested

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.viewpager2.nested.Vp2NestedScrollableHandler.Companion.defaultChildOrientation

/**
 * 将此类作为[RecyclerView]等滚动控件的容器，可处理[ViewPager2]嵌套滚动控件的滚动冲突。
 *
 * [Vp2NestedScrollableHandler]的注释解释了如何处理滚动冲突。
 *
 * @author xcc
 * @date 2022/7/8
 */
class Vp2NestedScrollableHost @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), Vp2NestedScrollableHandler.Host {
    private val handler = Vp2NestedScrollableHandler(
        childOrientation = { childOrientation(it) }
    )

    /**
     * 提供`child`的滚动方向，默认支持常用的滚动控件
     */
    var childOrientation: (child: View) -> Int = defaultChildOrientation

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        require(childCount <= 1) { "Vp2NestedScrollableHost只能有一个子View" }
        getChildAt(0)?.let { child -> handler.handleInterceptTouchEvent(child, e) }
        return super.onInterceptTouchEvent(e)
    }
}