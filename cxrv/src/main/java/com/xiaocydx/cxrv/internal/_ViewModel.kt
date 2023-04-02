@file:JvmName("_ViewModelInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.lifecycle

internal fun <T> ViewModel.setTagIfAbsent(key: String, value: T): T? = setTagIfAbsent(key, value)

internal fun <T> ViewModel.getTag(key: String): T? = getTag(key)