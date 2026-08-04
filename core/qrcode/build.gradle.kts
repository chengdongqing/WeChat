plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "top.chengdongqing.wechat.core.qrcode"
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
    implementation(projects.core.designsystem)
    implementation(projects.core.media)
    implementation(projects.core.mediaUi)
    implementation(projects.core.playback)
    implementation(projects.core.proximityUi)
    implementation(projects.core.util)
    implementation(libs.core.ktx)
    implementation(libs.bundles.camera)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
