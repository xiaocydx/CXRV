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

package com.xiaocydx.cxrv.multitype

/**
 * 可变的多类型容器
 *
 * @author xcc
 * @date 2021/10/9
 */
abstract class MutableMultiType<T : Any> : MultiType<T> {

    /**
     * 注册[T]的[type]
     *
     * 若对[T]注册多个[type]，则[T]属于一对多关系，
     * [ViewTypeDelegate.typeLinker]不能为空，否则抛出[IllegalStateException]异常。
     */
    @PublishedApi
    internal abstract fun register(type: Type<out T>)
}

/**
 * 注册[T1]的[ViewTypeDelegate]
 *
 * 若对[T1]注册多个[ViewTypeDelegate]，则[T1]属于一对多关系，
 * [ViewTypeDelegate.typeLinker]不能为空，否则抛出[IllegalStateException]异常。
 *
 * 一对一：
 * ```
 * sealed class Message {
 *     data class Text(val id: Int, val content: String): Message()
 *     data class Image(val id: Int, val image: Int): Message()
 * }
 *
 * listAdapter<Message> {
 *     register(TextDelegate())
 *     register(ImageDelegate())
 * }
 * ```
 *
 * 一对多：
 * ```
 * data class Message(
 *     val id: Int,
 *     val type: String, // text、image
 *     val content: String,
 *     val image: Int
 * )
 *
 * listAdapter<Message> {
 *     register(TextDelegate()) { it.type == "text" }
 *     register(ImageDelegate()) { it.type == "image" }
 * }
 * ```
 * @param delegate [T1]的[ViewTypeDelegate]
 */
inline fun <T : Any, reified T1 : T> MutableMultiType<T>.register(
    delegate: ViewTypeDelegate<T1, *>
) = register(Type(T1::class.java, delegate))

/**
 * 注册[T1]的[ViewTypeDelegate]，添加一对多判断条件的便捷方式
 *
 * 一对多：
 * ```
 * data class Message(
 *     val id: Int,
 *     val type: String, // text、image
 *     val content: String,
 *     val image: Int
 * )
 *
 * listAdapter<Message> {
 *     register(TextDelegate()) { it.type == "text" }
 *     register(ImageDelegate()) { it.type == "image" }
 * }
 * ```
 * @param delegate [T1]的[ViewTypeDelegate]
 * @param linker   [ViewTypeDelegate]的类型链接器
 */
inline fun <T : Any, reified T1 : T> MutableMultiType<T>.register(
    delegate: ViewTypeDelegate<T1, *>,
    noinline linker: (item: T1) -> Boolean
) = register(Type(T1::class.java, delegate.typeLinker(linker)))