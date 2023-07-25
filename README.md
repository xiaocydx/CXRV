CXRV是一个基于RecyclerView 1.2.0，提供常用功能的库，全称是Coroutines and Extensions for RecyclerView
<br><br>
[CXRV（一）概述](https://www.yuque.com/u12192380/khwdgb/fe9gsu)

[CXRV（二）ListAdapter的使用说明](https://www.yuque.com/u12192380/khwdgb/rpbw6f)

[CXRV（三）ViewTypeDelegate的使用说明](https://www.yuque.com/u12192380/khwdgb/qkpmiu)

[CXRV（四）扩展的使用说明](https://www.yuque.com/u12192380/khwdgb/kcxn6o)

[CXRV（五）ListState的使用说明](https://www.yuque.com/u12192380/khwdgb/uvgw43)

[CXRV（六）分页组件的使用说明](https://www.yuque.com/u12192380/khwdgb/gh9sbc)
<br><br>
1. 在根目录的build.gradle添加
```
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. 在module的build.gradle添加
```
dependencies {
    // RecyclerView的版本需要1.2.0或以上
    implementation "androidx.recyclerview:recyclerview:1.2.0"
    
    def version = "1.3.5"
    implementation "com.github.xiaocydx.CXRV:cxrv:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-binding:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-paging:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-animatable:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-viewpager2:${version}"
}
```
