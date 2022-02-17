@file:Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.xiaocydx.recycler.list

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [ReadOnlyList]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
class ReadOnlyListTest {
    private val mutableList = mutableListOf("A", "B", "C")
    private val kotlinList = mutableList.toReadOnlyList()
    private val javaList = kotlinList as java.util.List<String>

    @Test
    fun kotlin_ReadOnlyList_Immutable() {
        assertThat(kotlinList as? MutableList<*>).isNull()
    }

    @Test
    fun kotlin_ReadOnlyList_Iterator_Immutable() {
        assertThat(kotlinList.iterator() as? MutableIterable<*>).isNull()
    }

    @Test
    fun kotlin_ReadOnlyList_ListIterator_Immutable() {
        assertThat(kotlinList.listIterator() as? MutableIterable<*>).isNull()
        assertThat(kotlinList.listIterator(0) as? MutableListIterator<*>).isNull()
    }

    @Test
    fun java_ReadOnlyList_Unsupported_Add() {
        var exception: UnsupportedOperationException? = null
        try {
            javaList.add("D")
        } catch (e: UnsupportedOperationException) {
            exception = e
        }
        assertThat(exception).isNotNull()
    }

    @Test
    fun java_ReadOnlyList_Unsupported_IteratorRemove() {
        var exception: UnsupportedOperationException? = null
        try {
            javaList.iterator().remove()
        } catch (e: UnsupportedOperationException) {
            exception = e
        }
        assertThat(exception).isNotNull()
    }

    @Test
    fun java_ReadOnlyList_Unsupported_ListIteratorRemove() {
        var exception: UnsupportedOperationException? = null
        try {
            javaList.listIterator().remove()
        } catch (e: UnsupportedOperationException) {
            exception = e
        }
        assertThat(exception).isNotNull()

        try {
            exception = null
            javaList.listIterator(0).remove()
        } catch (e: UnsupportedOperationException) {
            exception = e
        }
        assertThat(exception).isNotNull()
    }
}