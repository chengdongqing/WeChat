plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat.core.location"
    compileSdk = 37

    defaultConfig {
        minSdk = 26

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    flavorDimensions.add("version")
    productFlavors {
        create("full") { dimension = "version" }
        create("lite") { dimension = "version" }
    }
    compileSdkMinor = 0
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.datastore.preferences)

    // 高德地图合包
    "fullImplementation"(files("${rootProject.projectDir}/core/location/libs/AMap3DMap_11.1.063_AMapSearch_9.7.4_AMapLocation_11.1.060_20260206.aar"))
    "liteImplementation"(files("${rootProject.projectDir}/core/location/libs/Lite3dMap_1.2.0_AMapSearch_9.7.4_AMapLocation_11.1.000_20260306.aar"))
}