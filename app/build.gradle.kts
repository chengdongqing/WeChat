plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.baselineprofile)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "top.chengdongqing.wechat"
        minSdk = 26
        targetSdk = 37
        versionCode = 20260802
        versionName = "2026.08.02"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

dependencies {
    // Feature modules
    implementation(projects.feature.launch)
    implementation(projects.feature.auth)
    implementation(projects.feature.chat)
    implementation(projects.feature.contacts)
    implementation(projects.feature.discovery)
    implementation(projects.feature.moments)
    implementation(projects.feature.intercom)
    implementation(projects.feature.profile)
    implementation(projects.feature.favorites)
    implementation(projects.feature.call)
    implementation(projects.feature.settings)
    implementation(projects.feature.common)

    // Core modules
    implementation(projects.core.designsystem)
    implementation(projects.core.common)
    implementation(projects.core.navigation)
    implementation(projects.core.network)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.location)
    implementation(projects.core.model)

    // 核心库
    implementation(libs.core.ktx)
    implementation(libs.annotations)
    implementation(libs.appcompat)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // 数据存储与序列化
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // 导航
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)

    // 依赖注入
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.profileinstaller)
    baselineProfile(projects.benchmark)

    // 测试相关
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(libs.test.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // 调试工具
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}
