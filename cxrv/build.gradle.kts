import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.xiaocydx.cxrv"
    kotlinOptions { jvmTarget = Versions.jvmTarget }
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
    configurations {
        testImplementation.extendsFrom(compileOnly)
        androidTestImplementation.extendsFrom(compileOnly)
    }
}
dependencies {
    implementation(PublishLibs.`androidx-recyclerview`)
    implementation(PublishLibs.`androidx-lifecycle-common`)
    implementation(PublishLibs.`androidx-lifecycle-viewmodel`)
    implementation(PublishLibs.coroutines)
    implementation(PublishLibs.`coroutines-android`)
    testImplementation(PublishLibs.`androidx-appcompat`)
    testImplementation(PublishLibs.`androidx-test-core`)
    testImplementation(PublishLibs.truth)
    testImplementation(PublishLibs.mockk)
    testImplementation(PublishLibs.robolectric)
    androidTestImplementation(PublishLibs.truth)
    androidTestImplementation(PublishLibs.`androidx-junit`)
    androidTestImplementation(PublishLibs.`androidx-appcompat`)
    androidTestImplementation(PublishLibs.`androidx-espresso-core`)
}