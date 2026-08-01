plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat.core.common"
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

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    api(projects.core.util)

    implementation(libs.core.ktx)
    implementation(libs.annotations)

    implementation(libs.bundles.camera)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.pinyin)
    implementation(libs.serialization.json)

    implementation(libs.bundles.coil)
    implementation(libs.coil.zoomable)

    implementation(libs.exifinterface)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
}
