package com.xiaocydx.recycler.paging

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter

/**
 * 加载视图容器
 *
 * @author xcc
 * @date 2021/12/2
 */
abstract class LoadLayout internal constructor(
    context: Context,
    private val loadingItem: LoadViewItem<out View>?,
    private val successItem: LoadViewItem<out View>?,
    private val failureItem: LoadViewItem<out View>?
) : FrameLayout(context) {
    private var showAction: Runnable? = null
    private var showType: ShowType = ShowType.NONE
    private val isShowLayout: Boolean
        get() = isVisible && showType != ShowType.NONE

    constructor(
        context: Context,
        loadingScope: LoadViewScope<out View>?,
        successScope: LoadViewScope<out View>?,
        failureScope: LoadViewScope<out View>?,
    ) : this(
        context = context,
        loadingItem = loadingScope?.getViewItem(),
        successItem = successScope?.getViewItem(),
        failureItem = failureScope?.getViewItem()
    )

    protected open fun postShowLoading() {
        if (!setShowType(ShowType.LOADING)) {
            return
        }
        hideAll()
        loadingItem?.postShow()
    }

    protected open fun postShowSuccess() {
        if (!setShowType(ShowType.SUCCESS)) {
            return
        }
        hideAll()
        successItem?.postShow()
    }

    protected open fun postShowFailure(exception: Throwable) {
        if (!setShowType(ShowType.FAILURE)) {
            return
        }
        hideAll()
        failureItem?.postShow(exception)
    }

    protected open fun postHideAll() {
        if (!setShowType(ShowType.NONE)) {
            return
        }
        hideAll()
    }

    private fun hideAll() {
        removeShowAction()
        loadingItem?.setVisible(false)
        successItem?.setVisible(false)
        failureItem?.setVisible(false)
    }

    /**
     * 若该函数在[Adapter.onBindViewHolder]下被调用，则此时itemView还未被添加到RecyclerView中,
     * 加载视图可能在可视时，判断[isShown]为true才执行某些操作，例如[LottieAnimationView.playAnimation]，
     * 因此当[isShown]为false时，发送消息执行[block]，消息被执行时，itemView已被添加到RecyclerView中。
     */
    @Suppress("KDocUnresolvedReference")
    private inline fun postShowAction(crossinline block: () -> Unit) {
        if (isShown) {
            block()
        } else {
            showAction = Runnable {
                showAction = null
                block()
            }
            post(showAction)
        }
    }

    private fun removeShowAction() {
        if (showAction != null) {
            removeCallbacks(showAction)
        }
    }

    private fun setShowType(type: ShowType): Boolean {
        if (showType != type) {
            showType = type
            return true
        }
        return false
    }

    private fun <V : View> LoadViewItem<V>.setVisible(
        isVisible: Boolean,
        exception: Throwable? = null
    ) {
        setVisible(parent = this@LoadLayout, isVisible, exception)
    }

    private fun <V : View> LoadViewItem<V>.postShow(exception: Throwable? = null) {
        postShowAction { setVisible(true, exception) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isShowLayout) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            setMeasuredDimension(0, 0)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (isShowLayout) {
            super.onLayout(changed, left, top, right, bottom)
        }
    }

    override fun draw(canvas: Canvas?) {
        if (isShowLayout) {
            super.draw(canvas)
        }
    }

    private enum class ShowType {
        NONE, LOADING, FAILURE, SUCCESS
    }
}