@file:SuppressLint("ViewConstructor")

package com.xiaocydx.recycler.paging

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.isFirstItemCompletelyVisible
import com.xiaocydx.recycler.marker.RvDslMarker

/**
 * 加载尾部视图容器
 *
 * @author xcc
 * @date 2021/11/24
 */
class LoadFooter internal constructor(
    context: Context,
    config: Config
) : LoadLayout(
    context = context,
    loadingScope = config.loadingScope,
    successScope = config.fullyScope,
    failureScope = config.failureScope
) {
    private val isShowFullyWhileExceed = config.isShowFullyWhileExceed

    init {
        layoutParams = LayoutParams(config.footerWidth, config.footerHeight)
    }

    public override fun postShowLoading() {
        super.postShowLoading()
    }

    public override fun postShowFailure(exception: Throwable) {
        super.postShowFailure(exception)
    }

    /**
     * 在[LoadFooterAdapter.onBindViewHolder]下调用
     */
    fun tryPostShowFully(recyclerView: RecyclerView) {
        if (!isShowFullyWhileExceed) {
            postShowSuccess()
            return
        }
        // 此时LoadFooter已算入itemCount，但还未被添加到RecyclerView中，
        // 因此将childCount + 1后再跟itemCount进行比较。
        val childCount = recyclerView.childCount + 1
        val itemCount = recyclerView.layoutManager!!.itemCount
        if (childCount >= itemCount && recyclerView.isFirstItemCompletelyVisible) {
            postHideAll()
        } else {
            postShowSuccess()
        }
    }

    @RvDslMarker
    data class Config
    @PublishedApi internal constructor(
        @PublishedApi
        internal var loadingScope: LoadViewScope.Normal<out View>? = null,
        @PublishedApi
        internal var fullyScope: LoadViewScope.Normal<out View>? = null,
        @PublishedApi
        internal var failureScope: LoadViewScope.Exception<out View>? = null,
        @Px
        private var _footerWidth: Int = MATCH_PARENT,
        @Px
        private var _footerHeight: Int = WRAP_CONTENT,
        private var _isShowFullyWhileExceed: Boolean = false
    ) {
        /**
         * 加载尾部的宽度
         */
        @get:Px
        @setparam:Px
        var footerWidth: Int
            get() = _footerWidth
            set(value) {
                _footerWidth = value
            }

        /**
         * 加载尾部的高度
         */
        @get:Px
        @setparam:Px
        var footerHeight: Int
            get() = _footerHeight
            set(value) {
                _footerHeight = value
            }

        /**
         * 是否超过RecyclerView可显示范围时才显示加载完全视图
         */
        var isShowFullyWhileExceed: Boolean
            get() = _isShowFullyWhileExceed && fullyScope != null
            set(value) {
                _isShowFullyWhileExceed = value
            }

        /**
         * 加载中视图
         *
         * ```
         * loading<ProgressBar> {
         *     onCreateView { parent -> ProgressBar(parent.context) }
         *     onBindView { view -> ... }
         * }
         * ```
         */
        inline fun <V : View> loading(block: LoadViewScope.Normal<V>.() -> Unit) {
            loadingScope = LoadViewScope.Normal<V>().apply(block)
        }

        /**
         * 加载中视图
         *
         * 若[block]为null，则表示不需要加载中视图。
         * ```
         * loadingView { parent -> ProgressBar(parent.context) }
         * ```
         */
        fun loadingView(block: OnCreateLoadView<out View>?) {
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
         *     onBindView { view -> view.text = "fully" }
         * }
         * ```
         */
        inline fun <V : View> fully(block: LoadViewScope.Normal<V>.() -> Unit) {
            fullyScope = LoadViewScope.Normal<V>().apply(block)
        }

        /**
         * 加载完全视图
         *
         * 若[block]为null，则表示不需要加载完全视图。
         * ```
         * fullyView { parent -> TextView(parent.context) }
         * ```
         */
        fun <V : View> fullyView(block: OnCreateLoadView<V>?) {
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
         *     onBindView { view, exception -> view.text = "failure" }
         * }
         * ```
         */
        inline fun <V : View> failure(block: LoadViewScope.Exception<V>.() -> Unit) {
            failureScope = LoadViewScope.Exception<V>().apply(block)
        }

        /**
         * 加载失败视图
         *
         * 若[block]为null，则表示不需要加载失败视图。
         * ```
         * failureView { parent -> TextView(parent.context) }
         * ```
         */
        fun failureView(block: OnCreateLoadView<out View>?) {
            if (block == null) {
                failureScope = null
            } else {
                failure<View> { onCreateView(block) }
            }
        }

        internal fun setCollector(collector: PagingCollector<*>) {
            loadingScope?.setCollector(collector)
            fullyScope?.setCollector(collector)
            failureScope?.setCollector(collector)
        }
    }
}