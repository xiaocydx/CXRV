@file:JvmName("ViewModelInternal")
@file:Suppress("PackageDirectoryMismatch")

package androidx.lifecycle

@PublishedApi
internal fun <T> ViewModel.setTagIfAbsent(key: String, value: T): T {
    return setTagIfAbsent(key, value)
}

@PublishedApi
internal fun <T> ViewModel.getTag(key: String): T? {
    return getTag(key)
}

@PublishedApi
internal fun ViewModel.clear() {
    clear()
}