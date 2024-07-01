plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xiaocydx.sample"
    defaultConfig { applicationId = "com.xiaocydx.sample" }
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":app-accompanist"))
    implementation(project(":cxrv"))
    implementation(project(":cxrv-paging"))
    implementation(project(":cxrv-binding"))
    implementation(project(":cxrv-viewpager2"))
    implementation(project(":cxrv-animatable"))
    debugImplementation(CommonLibs.leakcanary)
}