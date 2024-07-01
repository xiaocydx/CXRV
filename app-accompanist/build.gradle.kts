plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.accompanist"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    compileOnly(project(":cxrv"))
    compileOnly(project(":cxrv-paging"))
    compileOnly(project(":cxrv-viewpager2"))
    api(CommonLibs.insets)
    api(CommonLibs.`insets-compat`)
    api(CommonLibs.`insets-systembar`)
    api(CommonLibs.`androidx-core-ktx`)
    api(CommonLibs.`androidx-appcompat`)
    api(CommonLibs.`androidx-fragment`)
    api(CommonLibs.`androidx-fragment-ktx`)
    api(CommonLibs.`androidx-lifecycle-runtime-ktx`)
    api(CommonLibs.`androidx-transition`)
    api(CommonLibs.`androidx-recyclerview`)
    api(CommonLibs.`androidx-drawerlayout`)
    api(CommonLibs.`androidx-constraintlayout`)
    api(CommonLibs.`androidx-swiperefreshlayout`)
    api(CommonLibs.material)
    api(CommonLibs.retrofit)
    api(CommonLibs.`retrofit-converter-gson`)
    api(CommonLibs.glide)
    api(CommonLibs.coroutines)
    api(CommonLibs.`coroutines-android`)
}