package com.xiaocydx.sample.viewpager2.animatable

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.list.Disposable

/**
 * [AnimatableMediator]的实现类
 *
 * @author xcc
 * @date 2022/7/23
 */
@PublishedApi
@Suppress("SpellCheckingInspection")
internal class AnimatableMediatorImpl(
    override val recyclerView: RecyclerView
) : AnimatableMediator, () -> Unit {
    private var providers: ArrayList<AnimatableProvider>? = null
    private var controllers: ArrayList<AnimatableController>? = null
    private var preDrawListener: PreDrawListener? = null
    override var isDisposed: Boolean = false
    override val isAllowStart: Boolean
        get() {
            controllers?.accessEach { if (!it.isAllowStart) return false }
            return true
        }

    fun attach(): Disposable {
        preDrawListener = PreDrawListener(recyclerView, action = this)
        isDisposed = false
        return this
    }

    // checkOnPreDraw()
    override fun invoke() {
        if (!isAllowStart) stopAll()
    }

    override fun start(child: View) {
        if (isAllowStart) startActual(child)
    }

    override fun stop(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers?.accessEach action@{
            val animatable = it.getAnimatable(holder) ?: return@action
            if (animatable.isRunning) animatable.stop()
        }
    }

    override fun startAll() {
        if (!isAllowStart) return
        val childCount = recyclerView.childCount
        for (index in 0 until childCount) {
            startActual(recyclerView.getChildAt(index))
        }
    }

    override fun stopAll() {
        val childCount = recyclerView.childCount
        for (index in 0 until childCount) {
            stop(recyclerView.getChildAt(index))
        }
    }

    override fun addProvider(provider: AnimatableProvider) {
        if (providers == null) {
            providers = ArrayList(2)
        }
        if (!providers!!.contains(provider)) {
            providers!!.add(provider)
        }
    }

    override fun removeProvider(provider: AnimatableProvider) {
        providers?.remove(provider)
    }

    override fun addController(controller: AnimatableController) {
        if (controllers == null) {
            controllers = ArrayList(2)
        }
        if (!controllers!!.contains(controller)) {
            controllers!!.add(controller)
        }
    }

    override fun removeController(controller: AnimatableController) {
        controllers?.remove(controller)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AnimatableController> findController(clazz: Class<T>): T? {
        controllers?.accessEach { if (clazz.isAssignableFrom(it.javaClass)) return it as T }
        return null
    }

    private fun startActual(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers?.accessEach action@{
            val animatable = it.getAnimatable(holder) ?: return@action
            if (!animatable.isRunning) animatable.start()
        }
    }

    private inline fun <T> ArrayList<T>.accessEach(action: (T) -> Unit) {
        for (index in this.indices) action(get(index))
    }

    override fun dispose() {
        providers?.accessEach { it.dispose() }
        controllers?.accessEach { it.dispose() }
        preDrawListener?.removeListener()
        providers = null
        controllers = null
        preDrawListener = null
        isDisposed = true
    }
}

/**
 * 实现逻辑改造自[OneShotPreDrawListener]
 */
private class PreDrawListener(
    private val view: View,
    private val action: (() -> Unit)? = null
) : ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
    private var isAddedPreDrawListener = false
    private var viewTreeObserver: ViewTreeObserver = view.viewTreeObserver

    init {
        addOnPreDrawListener()
        view.addOnAttachStateChangeListener(this)
    }

    override fun onPreDraw(): Boolean {
        action?.invoke()
        return true
    }

    override fun onViewAttachedToWindow(view: View) {
        viewTreeObserver = view.viewTreeObserver
        addOnPreDrawListener()
    }

    override fun onViewDetachedFromWindow(view: View) {
        // 从视图树中移除监听，避免出现内存泄漏问题
        removeOnPreDrawListener()
    }

    fun removeListener() {
        removeOnPreDrawListener()
        view.removeOnAttachStateChangeListener(this)
    }

    private fun addOnPreDrawListener() {
        if (isAddedPreDrawListener) return
        isAddedPreDrawListener = true
        viewTreeObserver.addOnPreDrawListener(this)
    }

    private fun removeOnPreDrawListener() {
        if (!isAddedPreDrawListener) return
        isAddedPreDrawListener = false
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(this)
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
    }
}