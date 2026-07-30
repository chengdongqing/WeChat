plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "top.chengdongqing.wechat.core.designsystem"
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
    api(projects.core.util)

    api(platform(libs.compose.bom))
    api(libs.bundles.compose)
    api(libs.compose.icons.extended)
    api(libs.permissions.accompanist)

    implementation(libs.bundles.coil)
    implementation(libs.coil.zoomable)
    implementation(libs.appcompat)
}
