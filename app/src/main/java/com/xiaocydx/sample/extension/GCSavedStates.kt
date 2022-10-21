package com.xiaocydx.sample.extension

import androidx.collection.LongSparseArray
import androidx.collection.keyIterator
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.reflect.Field

fun FragmentStateAdapter.gcSavedStates() {
    val mSavedStatesField: Field = try {
        FragmentStateAdapter::class.java.getDeclaredField("mSavedStates")
    } catch (e: NoSuchFieldException) {
        null
    } ?: return

    mSavedStatesField.isAccessible = true
    val mSavedStates =
            mSavedStatesField.get(this) as? LongSparseArray<*> ?: return

    for (itemId in mSavedStates.keys()) {
        if (!containsItem(itemId)) mSavedStates.remove(itemId)
    }
}

private fun LongSparseArray<*>.keys(): LongArray {
    var index = 0
    val keys = LongArray(size())
    for (key in keyIterator()) {
        keys[index] = key
        index++
    }
    return keys
}