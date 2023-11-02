/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.paging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [LoadStates]的单元测试
 *
 * @author xcc
 * @date 2023/11/2
 */
internal class LoadStatesTest {

    @Test
    fun refreshToSuccess() {
        var previous = LoadStates(LoadState.Incomplete, LoadState.Incomplete)
        var current = previous.copy(refresh = LoadState.Loading)
        assertThat(previous.refreshToSuccess(current)).isFalse()
        assertThat(previous.refreshToComplete(current)).isFalse()

        previous = current
        current = previous.copy(refresh = LoadState.Success(isFully = true))
        assertThat(previous.refreshToSuccess(current)).isTrue()
        assertThat(previous.refreshToComplete(current)).isTrue()
    }

    @Test
    fun refreshToFailure() {
        var previous = LoadStates(LoadState.Incomplete, LoadState.Incomplete)
        var current = previous.copy(refresh = LoadState.Loading)
        assertThat(previous.refreshToFailure(current)).isFalse()
        assertThat(previous.refreshToComplete(current)).isFalse()

        previous = current
        current = previous.copy(refresh = LoadState.Failure(RuntimeException()))
        assertThat(previous.refreshToFailure(current)).isTrue()
        assertThat(previous.refreshToComplete(current)).isTrue()
    }

    @Test
    fun refreshToFully() {
        var previous = LoadStates(LoadState.Incomplete, LoadState.Incomplete)
        var current = previous.copy(refresh = LoadState.Loading)
        assertThat(previous.refreshToFully(current)).isFalse()

        previous = current
        current = previous.copy(refresh = LoadState.Success(isFully = false))
        assertThat(previous.refreshToFully(current)).isFalse()

        current = previous.copy(refresh = LoadState.Success(isFully = true))
        assertThat(previous.refreshToFully(current)).isTrue()
    }

    @Test
    fun appendToSuccess() {
        var previous = LoadStates(LoadState.Success(isFully = false), LoadState.Incomplete)
        var current = previous.copy(append = LoadState.Loading)
        assertThat(previous.appendToSuccess(current)).isFalse()
        assertThat(previous.appendToComplete(current)).isFalse()

        previous = current
        current = previous.copy(append = LoadState.Success(isFully = true))
        assertThat(previous.appendToSuccess(current)).isTrue()
        assertThat(previous.appendToComplete(current)).isTrue()
    }

    @Test
    fun appendToFailure() {
        var previous = LoadStates(LoadState.Success(isFully = false), LoadState.Incomplete)
        var current = previous.copy(append = LoadState.Loading)
        assertThat(previous.appendToFailure(current)).isFalse()
        assertThat(previous.appendToComplete(current)).isFalse()

        previous = current
        current = previous.copy(append = LoadState.Failure(RuntimeException()))
        assertThat(previous.appendToFailure(current)).isTrue()
        assertThat(previous.appendToComplete(current)).isTrue()
    }

    @Test
    fun appendToFully() {
        var previous = LoadStates(LoadState.Success(isFully = false), LoadState.Incomplete)
        var current = previous.copy(append = LoadState.Loading)
        assertThat(previous.appendToFully(current)).isFalse()

        previous = current
        current = previous.copy(append = LoadState.Success(isFully = false))
        assertThat(previous.appendToFully(current)).isFalse()

        current = previous.copy(append = LoadState.Success(isFully = true))
        assertThat(previous.appendToFully(current)).isTrue()
    }
}