plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.xiaocydx.accompanist"
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    compileOnly(project(":cxrv"))
    compileOnly(project(":cxrv-paging"))
    compileOnly(project(":cxrv-viewpager2"))
    val version = "1.2.2"
    api("com.github.xiaocydx.Insets:insets:${version}")
    api("com.github.xiaocydx.Insets:insets-compat:${version}")
    api("com.github.xiaocydx.Insets:insets-systembar:${version}")
    api("androidx.core:core-ktx:1.7.0")
    api("androidx.appcompat:appcompat:1.6.1")
    api("androidx.fragment:fragment:1.6.2")
    api("androidx.fragment:fragment-ktx:1.6.2")
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    api("androidx.transition:transition:1.4.1")
    api("androidx.recyclerview:recyclerview:1.2.0")
    api("androidx.drawerlayout:drawerlayout:1.1.1")
    api("androidx.constraintlayout:constraintlayout:2.1.4")
    api("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    api("com.google.android.material:material:1.7.0")
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.squareup.retrofit2:converter-gson:2.9.0")
    api("com.github.bumptech.glide:glide:4.14.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}