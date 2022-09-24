package com.xiaocydx.cxrv.layout

import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.xiaocydx.cxrv.internal.accessEach
import com.xiaocydx.cxrv.internal.toArrayList
import java.util.*

private val extensions: ArrayList<LayoutManagerExtensions<*>> = ServiceLoader.load(
    LayoutManagerExtensions::class.java,
    LayoutManagerExtensions::class.java.classLoader
).iterator().asSequence().toArrayList()

internal fun LayoutManager.extensionsImpl(): LayoutManagerExtensions<LayoutManager>? {
    val clazz = javaClass
    extensions.accessEach { extensions ->
        if (!extensions.layoutClass.isAssignableFrom(clazz)) return@accessEach
        @Suppress("UNCHECKED_CAST")
        return extensions as LayoutManagerExtensions<LayoutManager>
    }
    return null
}

internal inline fun <R> LayoutManager.runExtensions(
    action: LayoutManagerExtensions<LayoutManager>.() -> R
): R? = try {
    extensionsImpl()?.action()
} catch (cause: Throwable) {
    throw LayoutManagerExtensionsException(cause)
}

/**
 * 避免基本类型的装箱和拆箱
 */
internal inline fun <R : Number> LayoutManager.runExtensionsPrimitive(
    default: R,
    action: LayoutManagerExtensions<LayoutManager>.() -> R
): R = try {
    extensionsImpl()?.action() ?: default
} catch (cause: Throwable) {
    throw LayoutManagerExtensionsException(cause)
}

internal class LayoutManagerExtensionsException(cause: Throwable) : RuntimeException(cause)