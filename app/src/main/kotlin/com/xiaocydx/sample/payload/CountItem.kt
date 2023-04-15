package com.xiaocydx.sample.payload

import androidx.annotation.CheckResult

/**
 * @author xcc
 * @date 2022/11/13
 */
data class CountItem(
    val id: String,
    val count1: Int = 0,
    val count2: Int = 0,
    val count3: Int = 0
) {
    @CheckResult
    fun incrementCount1() = copy(count1 = count1 + 1)

    @CheckResult
    fun incrementCount2() = copy(count2 = count2 + 1)

    @CheckResult
    fun incrementCount3() = copy(count3 = count3 + 1)
}