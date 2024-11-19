// /*
//  * Copyright 2022 xiaocydx
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *    http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
//
// package com.xiaocydx.accompanist.transition.transform
//
// import android.view.animation.AccelerateDecelerateInterpolator
// import androidx.fragment.app.Fragment
// import androidx.fragment.app.requireTransformRoot
// import androidx.transition.Transition
// import com.google.android.material.transition.MaterialContainerTransform
//
// /**
//  * 变换过渡动画的Receiver，[Fragment]实现该接口完成完成初始化配置
//  *
//  * @author xcc
//  * @date 2023/8/7
//  */
// interface TransformReceiver {
//
//     /**
//      * 在`Fragment.activity != null`时，设置`Fragment.enterTransition`
//      *
//      * @param block 可以调用[MaterialContainerTransform]声明的函数完成初始化配置
//      * @return 返回设置为`enterTransition`的[Transition]，可以对其修改属性和添加监听。
//      */
//     fun <R> R.setReceiverEnterTransition(
//         block: (MaterialContainerTransform.() -> Unit)? = null
//     ): Transition where R : Fragment, R : TransformReceiver {
//         val root = requireTransformRoot()
//         val transform = MaterialContainerTransform()
//         transform.interpolator = AccelerateDecelerateInterpolator()
//         block?.invoke(transform)
//         return root.createReceiverTransition(this, transform).also(::setEnterTransition)
//     }
// }