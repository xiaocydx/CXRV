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

package com.xiaocydx.cxrv.itemvisible

import com.xiaocydx.cxrv.layout.LayoutManagerExtensions

/**
 * @author xcc
 * @date 2022/11/22
 */
internal class TestLayoutManagerExtensions : LayoutManagerExtensions<TestLayoutManager> {

    override fun findFirstVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findFirstVisibleItemPosition()
    }

    override fun findFirstCompletelyVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findFirstCompletelyVisibleItemPosition()
    }

    override fun findLastVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findLastVisibleItemPosition()
    }

    override fun findLastCompletelyVisibleItemPosition(layout: TestLayoutManager): Int {
        return layout.findLastCompletelyVisibleItemPosition()
    }
}