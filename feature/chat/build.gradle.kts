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
    compileSdk = 37

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

    flavorDimensions.add("version")
    productFlavors {
        create("full") { dimension = "version" }
        create("lite") { dimension = "version" }
    }
    compileSdkMinor = 0
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.location)

    implementation(projects.feature.contacts)
    implementation(projects.feature.call)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.coil)
    implementation(libs.serialization.json)
    implementation(libs.bundles.room)
    implementation(libs.datastore.preferences)
    implementation(libs.pinyin)
}
