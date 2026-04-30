plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "top.chengdongqing.wechat.feature.settings"
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

    implementation(projects.feature.chat)
    implementation(projects.feature.contacts)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.bundles.room)
    implementation(libs.appcompat)
}
