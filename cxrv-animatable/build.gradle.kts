plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.cxrv.animatable"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
}

dependencies {
    compileOnly(project(":cxrv"))
    implementation(PublishLibs.`androidx-recyclerview`)
    implementation(PublishLibs.`androidx-viewpager2`)
}