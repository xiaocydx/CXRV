CXRV是一个基于RecyclerView 1.2.0，提供常用功能的库，全称是Coroutines and Extensions for RecyclerView
<br><br>
[CXRV - 概述](https://www.yuque.com/u12192380/khwdgb/fe9gsu)

[CXRV - ListAdapter](https://www.yuque.com/u12192380/khwdgb/rpbw6f)

[CXRV - ViewTypeDelegate](https://www.yuque.com/u12192380/khwdgb/qkpmiu)

[CXRV - MutableStateList](https://www.yuque.com/u12192380/khwdgb/uvgw43)

[CXRV - Extensions](https://www.yuque.com/u12192380/khwdgb/kcxn6o)

[CXRV - cxrv-binding](https://www.yuque.com/u12192380/zl0316/xp5scx5w0ruldfit)

[CXRV - cxrv-paging](https://www.yuque.com/u12192380/khwdgb/gh9sbc)

[CXRV - cxrv-animatable](https://www.yuque.com/u12192380/zl0316/wa169ok4b4ueaian)

[CXRV - cxrv-viewpager2](https://www.yuque.com/u12192380/zl0316/hvqgw0vmdvl7ipgb)

[CXRV - 常见问题](https://www.yuque.com/u12192380/khwdgb/davrngc6pginrq2w)
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
    def version = "1.5.11"
    implementation "com.github.xiaocydx.CXRV:cxrv:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-binding:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-paging:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-animatable:${version}"
    implementation "com.github.xiaocydx.CXRV:cxrv-viewpager2:${version}"
}
```
