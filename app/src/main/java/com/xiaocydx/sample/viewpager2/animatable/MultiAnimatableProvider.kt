@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.sample.viewpager2.animatable

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * 添加[ImageView]的[AnimatableProvider]
 *
 * 当[ViewHolder.getBindingAdapter]等于[adapter]时，才调用[provider]。
 */
inline fun <VH : ViewHolder> AnimatableMediator.registerImageView(
    adapter: Adapter<VH>,
    crossinline provider: VH.() -> ImageView?
): Disposable = registerProvider(adapter) {
    this.provider()?.drawable as? Animatable
}

/**
 * 添加[ImageView]的[AnimatableProvider]
 *
 * 当[ViewHolder.getBindingAdapter]等于`delegate.adapter`，
 * 并且[ViewHolder.getItemViewType]等于`delegate.viewType`时，
 * 才调用[provider]。
 */
inline fun <VH : ViewHolder> AnimatableMediator.registerImageView(
    delegate: ViewTypeDelegate<*, VH>,
    crossinline provider: VH.() -> ImageView?
): Disposable = registerProvider(delegate) {
    this.provider()?.drawable as? Animatable
}

/**
 * 添加[AnimatableProvider]，[adapter]帮助类型推导[VH]
 *
 * 当[ViewHolder.getBindingAdapter]等于[adapter]时，才调用[provider]。
 */
fun <VH : ViewHolder> AnimatableMediator.registerProvider(
    adapter: Adapter<VH>,
    provider: VH.() -> Animatable?
): Disposable = AnimatableProviderDisposable().attach(
    mediator = this,
    provider = AdapterProvider(adapter, provider)
)

/**
 * 添加[AnimatableProvider]，[delegate]帮助类型推导[VH]
 *
 * 当[ViewHolder.getBindingAdapter]等于`delegate.adapter`，
 * 并且[ViewHolder.getItemViewType]等于`delegate.viewType`时，
 * 才调用[provider]。
 */
fun <VH : ViewHolder> AnimatableMediator.registerProvider(
    delegate: ViewTypeDelegate<*, VH>,
    provider: VH.() -> Animatable?
): Disposable = AnimatableProviderDisposable().attach(
    mediator = this,
    provider = ViewTypeDelegateProvider(delegate, provider)
)

private class AdapterProvider<VH : ViewHolder>(
    private val adapter: Adapter<VH>,
    private val provider: VH.() -> Animatable?
) : AnimatableProvider {
    override var isDisposed: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun getAnimatable(holder: ViewHolder): Animatable? {
        if (holder.bindingAdapter != adapter) return null
        return provider(holder as VH)
    }

    override fun dispose() {
        isDisposed = true
    }
}

private class ViewTypeDelegateProvider<VH : ViewHolder>(
    private val delegate: ViewTypeDelegate<*, VH>,
    private val provider: VH.() -> Animatable?
) : AnimatableProvider {
    override var isDisposed: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun getAnimatable(holder: ViewHolder): Animatable? {
        if (holder.bindingAdapter != delegate.adapter) return null
        if (holder.itemViewType != delegate.viewType) return null
        return provider(holder as VH)
    }

    override fun dispose() {
        isDisposed = true
    }
}

private class AnimatableProviderDisposable : AnimatableProvider {
    private var mediator: AnimatableMediator? = null
    private var provider: AnimatableProvider? = null
    override val isDisposed: Boolean
        get() = mediator == null && provider == null

    fun attach(
        mediator: AnimatableMediator,
        provider: AnimatableProvider
    ): Disposable {
        this.mediator = mediator
        this.provider = provider
        mediator.addProvider(this)
        return this
    }

    override fun getAnimatable(holder: ViewHolder): Animatable? {
        return provider?.getAnimatable(holder)
    }

    override fun dispose() {
        mediator?.removeProvider(this)
        provider?.dispose()
        mediator = null
        provider = null
    }
}