package com.xiaocydx.sample.extension

import androidx.collection.LongSparseArray
import androidx.collection.size
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

    for (index in 0 until mSavedStates.size) {
        val itemId = mSavedStates.keyAt(index)
        if (!containsItem(itemId)) {
            mSavedStates.remove(itemId)
        }
    }
}