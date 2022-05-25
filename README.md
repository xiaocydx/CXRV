[CXRV结构](https://www.yuque.com/u12192380/khwdgb/nmmsg5)

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
    implementation 'com.github.xiaocydx:CXRV:1.0.0'
    // RecyclerView的版本需要1.2.0或以上
    implementation 'androidx.recyclerview:recyclerview:1.2.0'
}
```
