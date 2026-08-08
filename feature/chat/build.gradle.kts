plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat.feature.chat"
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
    implementation(projects.core.appPicker)
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.runtime)
    implementation(projects.core.file)
    implementation(projects.core.media)
    implementation(projects.core.mediaUi)
    implementation(projects.core.camera)
    implementation(projects.core.qrcode)
    implementation(projects.feature.common)
    implementation(projects.core.playback)
    implementation(projects.core.connectivity)
    implementation(projects.core.callUi)
    implementation(projects.core.datetime)
    implementation(projects.core.navigation)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.location)
    implementation(projects.core.ai)


    implementation(libs.navigation.runtime)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.coil)
    implementation(libs.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.pinyin)
    implementation(libs.webrtc)
}
