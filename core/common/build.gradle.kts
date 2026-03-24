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
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)

    implementation(libs.core.ktx)
    implementation(libs.annotations)

    implementation(libs.bundles.camera)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.navigation.compose)
    implementation(libs.pinyin)
    implementation(libs.serialization.json)

    implementation(libs.bundles.coil)
    implementation(libs.coil.zoomable)

    implementation(libs.exifinterface)

    coreLibraryDesugaring(libs.desugar.jdk)
}
