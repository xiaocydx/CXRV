package com.xiaocydx.cxrv.paging

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.FrameLayout
import androidx.core.view.OneShotPreDrawListener
import com.xiaocydx.cxrv.extension.isVisible
import com.xiaocydx.cxrv.marker.RvDslMarker

/**
 * 加载视图的构建作用域
 *
 * @author xcc
 * @date 2022/5/21
 */
@RvDslMarker
class LoadViewScope<V : View> @PublishedApi internal constructor() {
    private var isComplete = false
    private var retry: (() -> Unit)? = null
    private var exception: (() -> Throwable?)? = null
    private var onCreateView: OnCreateView<V>? = null
    private var onVisibleChanged: OnVisibleChanged<V>? = null

    /**
     * 重新加载，该函数会对加载状态做判断，避免冗余请求
     */
    fun retry() {
        retry?.invoke()
    }

    /**
     * 返回加载失败的异常，若不是加载失败，则返回`null`
     */
    fun exception(): Throwable? {
        return exception?.invoke()
    }

    /**
     * 首次显示加载视图时，调用[block]
     */
    fun onCreateView(block: OnCreateView<V>) {
        checkComplete()
        onCreateView = block
    }

    /**
     * 显示、隐藏加载视图时，调用[block]
     */
    fun onVisibleChanged(block: OnVisibleChanged<V>) {
        checkComplete()
        onVisibleChanged = block
    }

    internal fun complete(retry: () -> Unit, exception: () -> Throwable?) {
        checkComplete()
        isComplete = true
        this.retry = retry
        this.exception = exception
    }

    internal fun getViewItem(): LoadViewItem<V>? {
        if (onCreateView == null) return null
        return LoadViewItem(this, onCreateView!!, onVisibleChanged)
    }

    private fun checkComplete() {
        check(!isComplete) { "已完成加载视图的配置" }
    }
}

typealias OnCreateView<V> = LoadViewScope<out V>.(parent: ViewGroup) -> V
typealias OnVisibleChanged<V> = LoadViewScope<out V>.(view: V, isVisible: Boolean) -> Unit

internal class LoadViewItem<V : View>(
    private val scope: LoadViewScope<V>,
    private val onCreateView: OnCreateView<V>,
    private val onVisibleChanged: OnVisibleChanged<V>?,
    private var view: V? = null
) {

    fun setVisible(parent: ViewGroup, isVisible: Boolean) {
        val isFirstVisible = isVisible && view == null
        if (isFirstVisible) {
            view = onCreateView(scope, parent).also(parent::addView)
        }
        val view = view ?: return
        val isVisibleChanged = view.isVisible != isVisible
        if (isVisibleChanged) {
            view.isVisible = isVisible
        }
        if (isFirstVisible || isVisibleChanged) {
            onVisibleChanged?.invoke(scope, view, isVisible)
        }
    }
}

@SuppressLint("ViewConstructor")
internal class LoadViewLayout(
    context: Context,
    width: Int,
    height: Int,
    private val loadingItem: LoadViewItem<out View>?,
    private val successItem: LoadViewItem<out View>?,
    private val failureItem: LoadViewItem<out View>?
) : FrameLayout(context) {

    init {
        layoutParams = ViewGroup.LayoutParams(width, height)
    }

    fun loadingVisible() {
        allInvisible()
        loadingItem?.setVisible(true)
    }

    fun successVisible() {
        allInvisible()
        successItem?.setVisible(true)
    }

    fun failureVisible() {
        allInvisible()
        failureItem?.setVisible(true)
    }

    private fun allInvisible() {
        loadingItem?.setVisible(false)
        successItem?.setVisible(false)
        failureItem?.setVisible(false)
    }

    private fun <V : View> LoadViewItem<V>.setVisible(isVisible: Boolean) {
        setVisible(parent = this@LoadViewLayout, isVisible)
    }
}

/**
 * 实现逻辑copy自[OneShotPreDrawListener]
 */
internal class PreDrawListener(
    private val view: View,
    private val action: () -> Unit
) : OnPreDrawListener, OnAttachStateChangeListener {
    private var viewTreeObserver: ViewTreeObserver = view.viewTreeObserver

    init {
        viewTreeObserver.addOnPreDrawListener(this)
        view.addOnAttachStateChangeListener(this)
    }

    override fun onPreDraw(): Boolean {
        action()
        return true
    }

    override fun onViewAttachedToWindow(v: View) {
        viewTreeObserver = v.viewTreeObserver
    }

    override fun onViewDetachedFromWindow(v: View) {
        removeListener()
    }

    fun removeListener() {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(this)
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
        view.removeOnAttachStateChangeListener(this)
    }
}