@file:Suppress("PackageDirectoryMismatch")

package androidx.lifecycle

internal fun <T> ViewModel.setTagIfAbsent(key: String, value: T): T? {
    return setTagIfAbsent(key, value)
}

internal fun <T> ViewModel.getTag(key: String): T? {
    return getTag(key)
}