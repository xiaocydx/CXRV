package com.xiaocydx.cxrv.extension

import android.view.View
import android.view.ViewGroup

/**
 * clone当前ArrayList再遍历
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <T> ArrayList<T>.cloneAccessEach(action: (T) -> Unit) {
    (clone() as ArrayList<T>).accessEach(action)
}

/**
 * clone当前ArrayList再遍历
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <T> ArrayList<T>.cloneAccessEachIndexed(action: (index: Int, T) -> Unit) {
    (clone() as ArrayList<T>).accessEachIndexed(action)
}

/**
 * 用于频繁遍历访问元素的场景，减少迭代器对象的创建
 */
internal inline fun <T> ArrayList<T>.accessEach(action: (T) -> Unit) {
    for (index in this.indices) {
        action(get(index))
    }
}

/**
 * 用于频繁遍历访问元素的场景，减少迭代器对象的创建
 */
internal inline fun <T> ArrayList<T>.accessEachIndexed(action: (index: Int, T) -> Unit) {
    for (index in this.indices) {
        action(index, get(index))
    }
}

/**
 * 用于频繁遍历访问元素的场景，减少迭代器对象的创建
 */
internal inline fun <T> ArrayList<T>.reverseAccessEach(action: (T) -> Unit) {
    for (index in size - 1 downTo 0) {
        action(get(index))
    }
}

/**
 * 用于频繁遍历访问子View的场景，减少迭代器对象的创建
 */
internal inline fun ViewGroup.childEach(action: (View) -> Unit) {
    val childCount = childCount
    for (index in 0 until childCount) {
        action(getChildAt(index))
    }
}