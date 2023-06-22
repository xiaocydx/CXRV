package com.xiaocydx.sample.viewpager2.shared

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.Target
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.sample.*
import com.xiaocydx.sample.foo.Foo
import com.xiaocydx.sample.viewpager2.shared.intoIsolate

/**
 * 修复[FooListAdapterError]列举的问题
 *
 * 讲述`sharedRecycledViewPool`场景的一些注意事项：
 * 1. 除了有视图设置逻辑，还要有视图重置逻辑，逻辑对称才能避免内存泄漏问题。
 * 2. 若使用[Glide]对[ImageView]加载图片，则需要和父级关联或者做另外处理。
 *
 * @author xcc
 * @date 2022/8/6
 */
@Suppress("KDocUnresolvedReference")
class FooListAdapterFixed(
    /**
     * 当前[FooListFragment]会复用其它[FooListFragment]回收进`sharedRecycledViewPool`的[ImageView]，
     * [Glide]对被共享复用的[ImageView]再次加载图片时，未及时移除上一个[RequestManager]记录的[Target]，
     * 当上一个[RequestManager.onDestroy]被调用时，`clear(Target)`对被共享复用的[ImageView]设置占位图。
     */
    private val fragment: FooListFragment,
    private val categoryId: Long
) : ListAdapter<Foo, FooViewHolder>() {

    /**
     * 1. 将[RequestManager]跟[FooListFragment]的父级关联，规避未及时移除[Target]造成的问题。
     *
     * **注意**：跟父级关联后，[FooListFragment]销毁时不会取消请求，而是在[onBindViewHolder]中，
     * 对被共享复用的[ImageView]再次加载图片时，若请求不一致，则取消上一次的请求。
     */
    private val requestManager = when (fragment.parentFragment) {
        null -> Glide.with(fragment.requireActivity())
        else -> Glide.with(fragment.requireParentFragment())
    }

    /**
     * 2. [doOnItemClick]会在合适的时机会清除状态，
     * 避免`sharedRecycledViewPool`场景出现内存泄漏问题。
     * 或者不使用 [doOnItemClick]，而是在[onBindViewHolder]设置[OnClickListener]，
     * 在[onViewRecycled]置空[OnClickListener]，确保逻辑对称，避免内存泄漏问题。
     */
    init {
        doOnItemClick { holder, item ->
            val context = holder.itemView.context
            context.showToast("doOnItemClick ${fragment.TAG}-${item.name}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooViewHolder {
        Log.e(fragment.TAG, "onCreateView：${System.currentTimeMillis()}")
        return FooViewHolder(FooItemView(parent.context))
    }

    /**
     * 3. [RequestBuilder.intoIsolate]
     * 能解决调用`GifDrawable.stop()`、`WebpDrawable.stop()`停止动图，
     * 出现相同动图url的[ImageView]内容绘制混乱的问题。
     *
     * [RequestBuilder.intoIsolate]做的事：
     * 1. 对缓存键混入附带categoryId的`signature`。
     * 2. 继承[ImageViewTarget]，重写`equals()`和`hashCode()`。
     * 因为对缓存键混入了`signature`，所以对被共享复用的[ImageView]再次加载图片时，
     * 即使url跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
     *
     * **注意**：由于对缓存键混入了`signature`，并且重写了[ImageViewTarget]的`equals()`和`hashCode()`,
     * 因此[RequestBuilder.intoIsolate]也能解决上面提到的未及时清除[Target]造成的问题，可作为解决方案2。
     */
    override fun onBindViewHolder(
        holder: FooViewHolder,
        item: Foo
    ): Unit = with(holder.itemView as FooItemView) {
        textView.text = item.name
        requestManager.load(item.url).centerCrop()
            .placeholder(R.color.placeholder_color)
            .intoIsolate(imageView, categoryId)
    }

    override fun areItemsTheSame(oldItem: Foo, newItem: Foo): Boolean {
        return oldItem.id == newItem.id
    }
}