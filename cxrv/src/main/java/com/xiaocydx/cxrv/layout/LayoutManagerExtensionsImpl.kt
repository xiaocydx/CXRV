package com.xiaocydx.cxrv.layout

import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.xiaocydx.cxrv.internal.accessEach
import com.xiaocydx.cxrv.internal.toArrayList
import java.util.*

private val extensions: ArrayList<ExtensionsHolder> = ServiceLoader.load(
    LayoutManagerExtensions::class.java,
    LayoutManagerExtensions::class.java.classLoader
).iterator().asSequence().map(::ExtensionsHolder).toArrayList()

internal fun LayoutManager.extensionsImpl(): LayoutManagerExtensions<LayoutManager>? {
    extensions.accessEach { extensions -> extensions.get(javaClass)?.let { return it } }
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

private class ExtensionsHolder(private val extensions: LayoutManagerExtensions<*>) {
    private val layoutClass = runCatching { extensions.layoutClass }.getOrNull()

    fun get(clazz: Class<out LayoutManager>): LayoutManagerExtensions<LayoutManager>? {
        if (layoutClass == null || !layoutClass.isAssignableFrom(clazz)) return null
        @Suppress("UNCHECKED_CAST")
        return extensions as LayoutManagerExtensions<LayoutManager>
    }
}