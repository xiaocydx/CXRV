package com.xiaocydx.cxrv.paging

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.Px
import com.xiaocydx.cxrv.internal.RvDslMarker

/**
 * 加载尾部配置
 *
 * @author xcc
 * @date 2022/5/21
 */
@RvDslMarker
class LoadFooterConfig @PublishedApi internal constructor() {
    private var isComplete = false

    @PublishedApi
    internal var loadingScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    @PublishedApi
    internal var fullyScope: LoadViewScope<out View>? = null
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
     * 加载尾部的宽度
     */
    @get:Px
    @setparam:Px
    var width: Int = MATCH_PARENT
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载尾部的高度
     */
    @get:Px
    @setparam:Px
    var height: Int = WRAP_CONTENT
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 是否超过RecyclerView可视范围时才显示加载完全视图
     *
     * **注意**：若[isFullyVisibleWhileExceed]为`true`，则在列表更改时，
     * 为了判断准确，会先移除`loadFooter`，在RecyclerView布局流程完成后，
     * 再判断是否显示加载完全视图，若需要显示，则重新添加`loadFooter`，
     * 这个过程可能会因为`loadFooter`被临时移除，而产生滚动。
     */
    var isFullyVisibleWhileExceed: Boolean = false
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
     * 加载完全视图
     *
     * ```
     * fully<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onVisibleChanged { view, isVisible -> ... }
     * }
     * ```
     */
    inline fun <V : View> fully(block: LoadViewScope<V>.() -> Unit) {
        fullyScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载完全视图
     *
     * 若[block]为null，则表示不需要空结果视图。
     * ```
     * fullyView { parent -> TextView(parent.context) }
     * ```
     */
    fun fullyView(block: OnCreateView<out View>?) {
        if (block == null) {
            fullyScope = null
        } else {
            fully<View> { onCreateView(block) }
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
        fullyScope?.complete(retry, exception)
        failureScope?.complete(retry, exception)
    }

    private fun checkComplete() {
        check(!isComplete) { "已完成加载尾部配置" }
    }
}