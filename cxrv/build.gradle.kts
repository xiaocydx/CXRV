import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.xiaocydx"
            afterEvaluate { from(components["release"]) }
        }
    }
}

android {
    namespace = "com.xiaocydx.cxrv"
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFile("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
    configurations {
        testImplementation.extendsFrom(compileOnly)
        androidTestImplementation.extendsFrom(compileOnly)
    }
}
dependencies {
    compileOnly("androidx.lifecycle:lifecycle-common:2.1.0")
    compileOnly("androidx.lifecycle:lifecycle-viewmodel:2.1.0")
    compileOnly("androidx.recyclerview:recyclerview:1.2.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    val truth = "com.google.truth:truth:1.0"
    val appcompat = "androidx.appcompat:appcompat:1.2.0"
    testImplementation(truth)
    testImplementation(appcompat)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.robolectric:robolectric:4.10.3")
    androidTestImplementation(truth)
    androidTestImplementation(appcompat)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}