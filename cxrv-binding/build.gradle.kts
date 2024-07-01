plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.cxrv.binding"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    buildFeatures { viewBinding = true }
}

dependencies {
    compileOnly(project(":cxrv"))
    implementation(PublishLibs.`androidx-recyclerview`)
    implementation(PublishLibs.coroutines)
    implementation(PublishLibs.`coroutines-android`)
}