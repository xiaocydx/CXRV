plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.xiaocydx.sample"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.xiaocydx.sample"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":app-accompanist"))
    implementation(project(":cxrv"))
    implementation(project(":cxrv-paging"))
    implementation(project(":cxrv-binding"))
    implementation(project(":cxrv-viewpager2"))
    implementation(project(":cxrv-animatable"))
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.9.1")
}