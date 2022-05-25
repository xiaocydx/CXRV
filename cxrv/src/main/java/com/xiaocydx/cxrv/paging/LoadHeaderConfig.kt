package com.xiaocydx.cxrv.paging

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.Px
import com.xiaocydx.cxrv.marker.RvDslMarker

/**
 * 加载头部配置
 *
 * @author xcc
 * @date 2022/5/18
 */
@RvDslMarker
class LoadHeaderConfig @PublishedApi internal constructor() {
    private var isComplete = false

    @PublishedApi
    internal var loadingScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    @PublishedApi
    internal var emptyScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    @PublishedApi
    internal var failureScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载头部的宽度
     */
    @get:Px
    @setparam:Px
    var width: Int = MATCH_PARENT
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载头部的高度
     */
    @get:Px
    @setparam:Px
    var height: Int = MATCH_PARENT
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载中视图
     *
     * ```
     * loading<ProgressBar> {
     *     onCreateView { parent -> ProgressBar(parent.context) }
     *     onVisibleChanged { view, isVisible -> ... }
     * }
     * ```
     */
    inline fun <V : View> loading(block: LoadViewScope<V>.() -> Unit) {
        loadingScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载中视图
     *
     * 若[block]为null，则表示不需要加载中视图。
     * ```
     * loadingView { parent -> ProgressBar(parent.context) }
     * ```
     */
    fun loadingView(block: OnCreateView<View>?) {
        if (block == null) {
            loadingScope = null
        } else {
            loading<View> { onCreateView(block) }
        }
    }

    /**
     * 空结果视图
     *
     * ```
     * empty<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onVisibleChanged { view, isVisible -> ... }
     * }
     * ```
     */
    inline fun <V : View> empty(block: LoadViewScope<V>.() -> Unit) {
        emptyScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 空结果视图
     *
     * 若[block]为null，则表示不需要空结果视图。
     * ```
     * emptyView { parent -> TextView(parent.context) }
     * ```
     */
    fun emptyView(block: OnCreateView<out View>?) {
        if (block == null) {
            emptyScope = null
        } else {
            empty<View> { onCreateView(block) }
        }
    }

    /**
     * 加载失败视图
     *
     * ```
     * failure<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onVisibleChanged { view, isVisible -> exception() }
     * }
     * ```
     */
    inline fun <V : View> failure(block: LoadViewScope<V>.() -> Unit) {
        failureScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载失败视图
     *
     * 若[block]为null，则表示不需要加载失败视图。
     * ```
     * failureView { parent -> TextView(parent.context) }
     * ```
     */
    fun failureView(block: OnCreateView<out View>?) {
        if (block == null) {
            failureScope = null
        } else {
            failure<View> { onCreateView(block) }
        }
    }

    internal fun complete(retry: () -> Unit, exception: () -> Throwable?) {
        checkComplete()
        isComplete = true
        loadingScope?.complete(retry, exception)
        emptyScope?.complete(retry, exception)
        failureScope?.complete(retry, exception)
    }

    private fun checkComplete() {
        check(!isComplete) { "已完成加载头部配置" }
    }
}