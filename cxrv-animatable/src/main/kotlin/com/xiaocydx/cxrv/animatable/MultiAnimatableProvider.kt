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

@file:Suppress("SpellCheckingInspection", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.animatable

import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.annotation.FloatRange
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.internal.isVisible
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * 添加[ImageView]的[AnimatableProvider]
 *
 * @param provider      当`holder.bindingAdapter`等于[adapter]时，才调用[provider]。
 * @param visiableRatio [provider]返回的[ImageView]，其可视比例大于或等于该值才开始动图。
 */
fun <VH : ViewHolder> AnimatableMediator.registerImageView(
    adapter: Adapter<VH>,
    @FloatRange(from = 0.0, to = 1.0) visiableRatio: Float = 0f,
    provider: VH.() -> ImageView?
): Disposable = AnimatableProviderDisposable().attach(
    mediator = this,
    provider = AdapterProvider(adapter, visiableRatio, provider)
)

/**
 * 添加[ImageView]的[AnimatableProvider]
 *
 * @param provider      当`holder.bindingAdapter`等于`delegate.adapter`，
 * 并且`holder.viewType`等于`delegate.viewType`时，才调用[provider]。
 * @param visiableRatio [provider]返回的[ImageView]，其可视比例大于或等于该值才开始动图。
 */
fun <VH : ViewHolder> AnimatableMediator.registerImageView(
    delegate: ViewTypeDelegate<*, VH>,
    @FloatRange(from = 0.0, to = 1.0) visiableRatio: Float = 0f,
    provider: VH.() -> ImageView?
): Disposable = AnimatableProviderDisposable().attach(
    mediator = this,
    provider = ViewTypeDelegateProvider(delegate, visiableRatio, provider)
)

/**
 * 可废弃的[AnimatableProvider]
 */
class AnimatableProviderDisposable : AnimatableProvider {
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
        mediator.addAnimatableProvider(this)
        return this
    }

    override fun getAnimatableOrNull(holder: ViewHolder): Animatable? {
        return provider?.getAnimatableOrNull(holder)
    }

    override fun canStartAnimatable(holder: ViewHolder, animatable: Animatable): Boolean {
        return provider?.canStartAnimatable(holder, animatable) ?: true
    }

    override fun dispose() {
        mediator?.removeAnimatableProvider(this)
        provider?.dispose()
        mediator = null
        provider = null
    }
}

private class AdapterProvider<VH : ViewHolder>(
    private val adapter: Adapter<VH>,
    visiableRatio: Float,
    provider: VH.() -> ImageView?
) : ImageViewProvider<VH>(visiableRatio, provider) {

    @Suppress("UNCHECKED_CAST")
    override fun getAnimatableOrNull(holder: ViewHolder): Animatable? {
        if (holder.bindingAdapter !== adapter) return null
        return provider(holder as VH)?.drawable as? Animatable
    }
}

private class ViewTypeDelegateProvider<VH : ViewHolder>(
    private val delegate: ViewTypeDelegate<*, VH>,
    visiableRatio: Float,
    provider: VH.() -> ImageView?
) : ImageViewProvider<VH>(visiableRatio, provider) {

    @Suppress("UNCHECKED_CAST")
    override fun getAnimatableOrNull(holder: ViewHolder): Animatable? {
        if (holder.bindingAdapter !== delegate.adapter) return null
        if (holder.itemViewType != delegate.viewType) return null
        return provider(holder as VH)?.drawable as? Animatable
    }
}

private abstract class ImageViewProvider<VH : ViewHolder>(
    private val visiableRatio: Float,
    protected val provider: VH.() -> ImageView?
) : AnimatableProvider {
    private val visibleRect = Rect()
    override var isDisposed: Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun canStartAnimatable(holder: ViewHolder, animatable: Animatable): Boolean {
        if (visiableRatio <= 0f) return true
        val view = provider(holder as VH)
        if (view == null || !view.isAttachedToWindow || !view.isVisible) return false
        view.getLocalVisibleRect(visibleRect)
        val visibleArea = visibleRect.width() * visibleRect.height()
        val totalAre = view.width * view.height
        return (visibleArea.toFloat() / totalAre) >= visiableRatio
    }

    override fun dispose() {
        isDisposed = true
    }
}