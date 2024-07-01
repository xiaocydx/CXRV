import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.cxrv.viewpager2"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
    configurations {
        testImplementation.extendsFrom(compileOnly)
    }
}

dependencies {
    implementation(PublishLibs.`androidx-recyclerview`)
    implementation(PublishLibs.`androidx-viewpager2`)
    testImplementation(PublishLibs.`androidx-appcompat`)
    testImplementation(PublishLibs.`androidx-test-core`)
    testImplementation(PublishLibs.truth)
    testImplementation(PublishLibs.mockk)
    testImplementation(PublishLibs.robolectric)
}