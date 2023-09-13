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

package com.xiaocydx.sample.paging.complex.transform

import android.view.View
import android.view.WindowInsets
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlin.reflect.KClass

/**
 * 变换过渡动画的Container，[FragmentActivity]或者[Fragment]实现该接口完成初始化配置
 *
 * @author xcc
 * @date 2023/8/7
 */
interface TransformContainer {

    /**
     * 将[FragmentActivity]作为[TransformContainer]的载体，设置`ContentView`
     *
     * ```
     * class TransformContainerActivity : AppCompatActivity(), TransformContainer {
     *
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *         setContentView(ContentFragment::class)
     *     }
     * }
     * ```
     */
    fun <C> C.setContentView(
        fragmentClass: KClass<out Fragment>
    ) where C : FragmentActivity, C : TransformContainer {
        setContentView(createContentView(fragmentClass))
    }

    /**
     * 将[FragmentActivity]作为[TransformContainer]的载体，创建`ContentView`
     *
     * ```
     * // BaseActivity为已有的基类，子类需要实现createContentView()创建ContentView
     * class TransformContainerActivity : BaseActivity(), TransformContainer {
     *
     *     override fun createContentView() = createContentView(ContentFragment::class)
     * }
     * ```
     */
    fun <C> C.createContentView(
        fragmentClass: KClass<out Fragment>
    ): View where C : FragmentActivity, C : TransformContainer {
        return TransformSceneRoot(this, supportFragmentManager)
            .apply { installOnAttach(fragmentClass) }.toContentView()
    }

    /**
     * 将[Fragment]作为[TransformContainer]的载体，创建`ContentView`
     *
     * ```
     * class TransformContainerFragment : Fragment(), TransformContainer {
     *
     *     override fun onCreateView(
     *         inflater: LayoutInflater,
     *         container: ViewGroup?,
     *         savedInstanceState: Bundle?
     *     ): View = createContentView(ContentFragment::class)
     * }
     * ```
     */
    fun <C> C.createContentView(
        fragmentClass: KClass<out Fragment>
    ): View where C : Fragment, C : TransformContainer {
        val primaryNavigationFragment = this
        return TransformSceneRoot(requireContext(), childFragmentManager)
            .apply { installOnAttach(fragmentClass, primaryNavigationFragment) }
            .toContentView()
    }

    /**
     * 禁用`Window.decorView`对[WindowInsets]的处理，去除间距实现和系统栏背景色
     *
     * ```
     * class TransformContainerActivity : AppCompatActivity(), TransformContainer {
     *
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *         disableDecorFitsSystemWindows()
     *         setContentView(ContentFragment::class)
     *     }
     * }
     * ```
     */
    fun <C> C.disableDecorFitsSystemWindows() where C : FragmentActivity, C : TransformContainer {
        SystemBarsContainer.disableDecorFitsSystemWindows(window)
    }
}

internal fun Fragment.requireTransformSceneRoot(): TransformSceneRoot {
    return requireNotNull(findTransformSceneRoot()) {
        "请先调用TransformContainer提供的FragmentActivity.setContentView()，" +
                "或者FragmentActivity.createContentView()，又或者Fragment.createContentView()。"
    }
}