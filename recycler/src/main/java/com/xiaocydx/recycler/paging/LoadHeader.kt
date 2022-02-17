@file:SuppressLint("ViewConstructor")

package com.xiaocydx.recycler.paging

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.Px
import com.xiaocydx.recycler.marker.RvDslMarker

/**
 * 加载头部视图容器
 *
 * @author xcc
 * @date 2021/11/21
 */
class LoadHeader internal constructor(
    context: Context,
    config: Config
) : LoadLayout(
    context = context,
    loadingScope = config.loadingScope,
    successScope = config.emptyScope,
    failureScope = config.failureScope
) {

    init {
        layoutParams = LayoutParams(config.headerWidth, config.headerHeight)
    }

    public override fun postShowLoading() {
        super.postShowLoading()
    }

    public override fun postShowFailure(exception: Throwable) {
        super.postShowFailure(exception)
    }

    fun postShowEmpty() {
        postShowSuccess()
    }

    @RvDslMarker
    data class Config
    @PublishedApi internal constructor(
        @PublishedApi
        internal var loadingScope: LoadViewScope.Normal<out View>? = null,
        @PublishedApi
        internal var emptyScope: LoadViewScope.Normal<out View>? = null,
        @PublishedApi
        internal var failureScope: LoadViewScope.Exception<out View>? = null,
        @Px
        private var _headerWidth: Int = MATCH_PARENT,
        @Px
        private var _headerHeight: Int = MATCH_PARENT,
    ) {
        /**
         * 加载头部的宽度
         */
        @get:Px
        @setparam:Px
        var headerWidth: Int
            get() = _headerWidth
            set(value) {
                _headerHeight = value
            }

        /**
         * 加载头部的高度
         */
        @get:Px
        @setparam:Px
        var headerHeight: Int
            get() = _headerHeight
            set(value) {
                _headerHeight = value
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
         * 空结果视图
         *
         * ```
         * empty<TextView> {
         *     onCreateView { parent -> TextView(parent.context) }
         *     onBindView { view -> view.text = "empty" }
         * }
         * ```
         */
        inline fun <V : View> empty(block: LoadViewScope.Normal<V>.() -> Unit) {
            emptyScope = LoadViewScope.Normal<V>().apply(block)
        }

        /**
         * 空结果视图
         *
         * 若[block]为null，则表示不需要空结果视图。
         * ```
         * emptyView { parent -> TextView(parent.context) }
         * ```
         */
        fun emptyView(block: OnCreateLoadView<out View>?) {
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
         *     onBindView { view -> view.text = "failure" }
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
            emptyScope?.setCollector(collector)
            failureScope?.setCollector(collector)
        }
    }
}