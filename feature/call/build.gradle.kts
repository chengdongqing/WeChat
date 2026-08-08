plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "top.chengdongqing.wechat.feature.call"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
    compileSdkMinor = 0
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.runtime)
    implementation(projects.core.playback)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.data)

    implementation(libs.navigation.runtime)
    implementation(libs.core.telecom)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.serialization.json)

    implementation(libs.webrtc)

    implementation(libs.bundles.coil)
}
