plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat.core.model"
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
    implementation(libs.serialization.json)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.appcompat)
}
