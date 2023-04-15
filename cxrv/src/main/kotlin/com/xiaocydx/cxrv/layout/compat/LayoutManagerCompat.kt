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

@file:JvmName("LayoutManagerCompatInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.util.Log
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.LayoutManager

/**
 * 提供兼容属性的[LayoutManager]
 *
 * @author xcc
 * @date 2022/12/12
 */
interface LayoutManagerCompat {
    companion object
}

private const val TAG = "LayoutManagerCompat"

internal inline fun LayoutManagerCompat.Companion.warn(layout: LayoutManager, reason: () -> String = { "" }) {
    val content = content(layout) ?: return
    Log.w(TAG, combine(content, reason()), IllegalArgumentException())
}

internal inline fun LayoutManagerCompat.Companion.assert(layout: LayoutManager, reason: () -> String = { "" }) {
    val content = content(layout) ?: return
    throw AssertionError(combine(content, reason()))
}

private fun content(layout: LayoutManager): StringBuilder? {
    val functionName: String
    val layoutName: String
    when (layout) {
        is LayoutManagerCompat -> return null
        is GridLayoutManager -> {
            functionName = "grid"
            layoutName = GridLayoutManagerCompat::class.java.canonicalName ?: ""
        }
        is LinearLayoutManager -> {
            functionName = "linear"
            layoutName = LinearLayoutManagerCompat::class.java.canonicalName ?: ""
        }
        is StaggeredGridLayoutManager -> {
            functionName = "staggered"
            layoutName = StaggeredGridLayoutManagerCompat::class.java.canonicalName ?: ""
        }
        else -> return null
    }
    return StringBuilder()
        .append("请调用RecyclerView.")
        .append(functionName).append("()")
        .append("设置").append(layoutName)
}

private fun combine(content: StringBuilder, reason: String): String {
    if (reason.isNotEmpty()) content.append("，").append(reason)
    return content.toString()
}