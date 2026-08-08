plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "top.chengdongqing.wechat.feature.contacts"
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
    implementation(projects.core.data)
    implementation(projects.core.runtime)
    implementation(projects.core.file)
    implementation(projects.core.nfc)
    implementation(projects.core.qrcode)
    implementation(projects.core.callUi)
    implementation(projects.core.proximityUi)
    implementation(projects.core.datetime)
    implementation(projects.core.navigation)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.database)



    implementation(libs.navigation.runtime)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.serialization.json)

    implementation(libs.room.runtime)
    implementation(libs.bundles.coil)
    implementation(libs.coil.zoomable)

    implementation(libs.pinyin)
}
