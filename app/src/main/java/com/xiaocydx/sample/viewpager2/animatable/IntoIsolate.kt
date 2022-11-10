package com.xiaocydx.sample.viewpager2.animatable

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Key.CHARSET
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.ImageViewTarget
import java.security.MessageDigest

/**
 * 对[imageView]隔离加载图片
 *
 * 1. 对缓存键混入附带[isolateKey]的`signature`。
 * 2. 继承[ImageViewTarget]，重写[Object.equals]和[Object.hashCode]。
 *
 * **注意**：
 * 因为对缓存键混入了`signature`，所以对被共享复用的[imageView]再次加载图片时，
 * 即使`url`跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
 *
 * @param isolateKey 可以是item的id或者分组id，`isolateKey.toString()`的结果必须唯一。
 * @param apply      [ImageViewTarget.waitForLayout]需要在[apply]中调用。
 */
inline fun RequestBuilder<Bitmap>.intoIsolate(
    imageView: ImageView,
    isolateKey: Any,
    apply: ImageViewTarget<Bitmap>.() -> Unit = {}
): ImageViewTarget<Bitmap> {
    return signature(IsolateKeySignature(signature, isolateKey))
        .into(TimelyClearedBitmapImageViewTarget(imageView).also(apply))
}

/**
 * 对[imageView]隔离加载图片
 *
 * 1. 对缓存键混入附带[isolateKeys]的`signature`。
 * 2. 继承[ImageViewTarget]，重写[Object.equals]和[Object.hashCode]。
 *
 * **注意**：
 * 因为对缓存键混入了`signature`，所以对被共享复用的[imageView]再次加载图片时，
 * 即使`url`跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
 *
 * @param isolateKeys `isolateKey`可以是item的id或者分组id，`isolateKey.toString()`的结果必须唯一。
 * @param apply       [ImageViewTarget.waitForLayout]需要在[apply]中调用。
 */
inline fun RequestBuilder<Bitmap>.intoIsolate(
    imageView: ImageView,
    vararg isolateKeys: Any,
    apply: ImageViewTarget<Bitmap>.() -> Unit = {}
): ImageViewTarget<Bitmap> {
    return signature(IsolateKeysSignature(signature, isolateKeys))
        .into(TimelyClearedBitmapImageViewTarget(imageView).also(apply))
}

/**
 * 对[imageView]隔离加载图片
 *
 * 1. 对缓存键混入附带[isolateKey]的`signature`。
 * 2. 继承[ImageViewTarget]，重写[Object.equals]和[Object.hashCode]。
 *
 * **注意**：
 * 因为对缓存键混入了`signature`，所以对被共享复用的[imageView]再次加载图片时，
 * 即使`url`跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
 *
 * @param isolateKey 可以是item的id或者分组id，`isolateKey.toString()`的结果必须唯一。
 * @param apply      [ImageViewTarget.waitForLayout]需要在[apply]中调用。
 */
@JvmName("intoIsolateDrawable")
inline fun RequestBuilder<Drawable>.intoIsolate(
    imageView: ImageView,
    isolateKey: Any,
    apply: ImageViewTarget<Drawable>.() -> Unit = {}
): ImageViewTarget<Drawable> {
    return signature(IsolateKeySignature(signature, isolateKey))
        .into(TimelyClearedDrawableImageViewTarget(imageView).also(apply))
}

/**
 * 对[imageView]隔离加载图片
 *
 * 1. 对缓存键混入附带[isolateKeys]的`signature`。
 * 2. 继承[ImageViewTarget]，重写[Object.equals]和[Object.hashCode]。
 *
 * **注意**：
 * 因为对缓存键混入了`signature`，所以对被共享复用的[imageView]再次加载图片时，
 * 即使`url`跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
 *
 * @param isolateKeys `isolateKey`可以是item的id或者分组id，`isolateKey.toString()`的结果必须唯一。
 * @param apply       [ImageViewTarget.waitForLayout]需要在[apply]中调用。
 */
@JvmName("intoIsolateDrawable")
inline fun RequestBuilder<Drawable>.intoIsolate(
    imageView: ImageView,
    vararg isolateKeys: Any,
    apply: ImageViewTarget<Drawable>.() -> Unit = {}
): ImageViewTarget<Drawable> {
    return signature(IsolateKeysSignature(signature, isolateKeys))
        .into(TimelyClearedDrawableImageViewTarget(imageView).also(apply))
}

/**
 * data class实现[Key.equals]和[Key.hashCode]
 */
@PublishedApi
internal data class IsolateKeySignature(
    private val sourceKey: Key,
    private val isolateKey: Any
) : Key {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        sourceKey.updateDiskCacheKey(messageDigest)
        messageDigest.update(isolateKey.toString().toByteArray(CHARSET))
    }
}

@PublishedApi
internal class IsolateKeysSignature(
    private val sourceKey: Key,
    private val isolateKeys: Array<out Any>
) : Key {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        sourceKey.updateDiskCacheKey(messageDigest)
        messageDigest.update(isolateKeys.contentToString().toByteArray(CHARSET))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IsolateKeysSignature
        if (sourceKey != other.sourceKey) return false
        if (!isolateKeys.contentEquals(other.isolateKeys)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = sourceKey.hashCode()
        result = 31 * result + isolateKeys.contentHashCode()
        return result
    }
}

/**
 * data class重写[Object.equals]和[Object.hashCode]
 *
 * 重写[Object.equals]和[Object.hashCode]的目的，
 * 是让[Glide.removeFromManagers]能及时移除[Target]。
 */
@PublishedApi
internal data class TimelyClearedBitmapImageViewTarget(
    private val imageView: ImageView
) : BitmapImageViewTarget(imageView)

/**
 * data class重写[Object.equals]和[Object.hashCode]
 *
 * 重写[Object.equals]和[Object.hashCode]的目的，
 * 是让[Glide.removeFromManagers]能及时移除[Target]。
 */
@PublishedApi
internal data class TimelyClearedDrawableImageViewTarget(
    private val imageView: ImageView
) : DrawableImageViewTarget(imageView)