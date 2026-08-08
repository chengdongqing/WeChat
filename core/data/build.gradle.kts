plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "top.chengdongqing.wechat.core.data"
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
    compileSdkMinor = 0
}

dependencies {
    implementation(projects.core.appPicker)
    implementation(projects.core.file)
    implementation(projects.core.qrcode)
    implementation(projects.core.util)
    implementation(projects.core.playback)
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.notification)
    implementation(projects.core.datetime)

    implementation(libs.core.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.paging.compose)
    implementation(libs.room.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
