package com.xiaocydx.sample.animatable

import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import com.xiaocydx.accompanist.glide.intoIsolate
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.Foo
import com.xiaocydx.sample.common.gifUrls
import com.xiaocydx.sample.viewpager2.shared.FooItemView
import com.xiaocydx.sample.viewpager2.shared.FooViewHolder

/**
 * @author xcc
 * @date 2024/12/20
 */
class AnimatableMediatorAdapter(
    fragment: Fragment,
    private val categoryId: Long
) : ListAdapter<Foo, FooViewHolder>() {
    private val requestManager = Glide.with(fragment)

    init {
        submitList((1..20).map { num ->
            val url = gifUrls[num % gifUrls.size]
            Foo(id = num.toString(), name = "Foo-$num", num, url)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooViewHolder {
        return FooViewHolder(FooItemView(parent.context))
    }

    /**
     * [RequestBuilder.intoIsolate]：
     * 解决调用`GifDrawable.stop()`、`WebpDrawable.stop()`，
     * 出现相同动图url的[ImageView]内容绘制混乱的问题。
     *
     * [RequestBuilder.intoIsolate]做的事：
     * 1. 对缓存键混入附带[categoryId]的`signature`。
     * 2. 继承[ImageViewTarget]，重写`equals()`和`hashCode()`。
     * 混入`signature`后，其它[AnimatableMediatorFragment]加载相同的url，
     * 不会看作是同一请求，这在一定程度上降低了资源重用率。
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