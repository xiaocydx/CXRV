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

package com.xiaocydx.cxrv.list

/**
 * 提供列表状态相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2022/3/8
 */
internal interface ListMediator<T : Any> {
    /**
     * 列表版本号
     *
     * 若[updateList]更新成功，则增加版本号
     */
    val version: Int

    /**
     * 当前列表
     */
    val currentList: List<T>

    /**
     * 执行列表更新操作
     */
    fun updateList(op: UpdateOp<T>)
}