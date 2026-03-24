plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "top.chengdongqing.wechat.core.designsystem"
    compileSdk = 36

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
}

dependencies {
    implementation(projects.core.model)

    api(platform(libs.compose.bom))
    api(libs.bundles.compose)
    api(libs.compose.icons.extended)
    api(libs.compose.material3.adaptive)
    api(libs.permissions.accompanist)

    implementation(libs.bundles.coil)
    implementation(libs.coil.zoomable)
    implementation(libs.appcompat)
}
