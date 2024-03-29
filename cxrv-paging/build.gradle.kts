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
    namespace = "com.xiaocydx.cxrv.paging"
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
    }
}

dependencies {
    compileOnly(project(":cxrv"))
    implementation("androidx.recyclerview:recyclerview:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    testImplementation("androidx.appcompat:appcompat:1.2.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("com.google.truth:truth:1.0")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.robolectric:robolectric:4.10.3")
}