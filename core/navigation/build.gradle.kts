plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "top.chengdongqing.wechat.core.navigation"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.navigation.runtime)
    implementation(libs.serialization.json)
}
