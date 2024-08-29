CXRV是一个基于RecyclerView 1.2.0，提供常用功能的库，全称是Coroutines and Extensions for RecyclerView
<br><br>
[CXRV（一）概述](https://www.yuque.com/u12192380/khwdgb/fe9gsu)

[CXRV（二）ListAdapter](https://www.yuque.com/u12192380/khwdgb/rpbw6f)

[CXRV（三）ViewTypeDelegate](https://www.yuque.com/u12192380/khwdgb/qkpmiu)

[CXRV（四）扩展](https://www.yuque.com/u12192380/khwdgb/kcxn6o)

[CXRV（五）ListState](https://www.yuque.com/u12192380/khwdgb/uvgw43)

[CXRV（六）分页组件](https://www.yuque.com/u12192380/khwdgb/gh9sbc)

[CXRV（七）常见问题](https://www.yuque.com/u12192380/khwdgb/davrngc6pginrq2w)
<br><br>
1. 在根目录的settings.gradle添加
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

2. 在module的build.gradle添加
```
dependencies {
    def version = "1.5.6"
    implementation "com.github.xiaocydx.CXRV:cxrv:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-binding:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-paging:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-animatable:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-viewpager2:${version}"
}
```
