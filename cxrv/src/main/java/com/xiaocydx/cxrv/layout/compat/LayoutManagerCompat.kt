package com.xiaocydx.cxrv.layout.compat

import android.util.Log
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.LayoutManager

/**
 * 提供[LayoutManager]兼容场景所需的提示函数
 *
 * @author xcc
 * @date 2022/12/12
 */
internal object LayoutManagerCompat {
    private const val TAG = "LayoutManagerCompat"

    inline fun warn(layout: LayoutManager, reason: () -> String = { "" }) {
        val content = content(layout) ?: return
        Log.w(TAG, combine(content, reason()), IllegalArgumentException())
    }

    inline fun assert(layout: LayoutManager, reason: () -> String = { "" }) {
        val content = content(layout) ?: return
        throw AssertionError(combine(content, reason()))
    }

    private fun content(layout: LayoutManager): StringBuilder? {
        val functionName: String
        val layoutName: String
        when {
            layout is GridLayoutManager && layout !is GridLayoutManagerCompat -> {
                functionName = "grid"
                layoutName = GridLayoutManagerCompat::class.java.canonicalName ?: ""
            }
            layout is LinearLayoutManager && layout !is LinearLayoutManagerCompat -> {
                functionName = "linear"
                layoutName = LinearLayoutManagerCompat::class.java.canonicalName ?: ""
            }
            layout is StaggeredGridLayoutManager && layout !is StaggeredGridLayoutManagerCompat -> {
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
}