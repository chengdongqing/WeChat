import androidx.room.gradle.RoomExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat"
    compileSdk = 36

    defaultConfig {
        applicationId = "top.chengdongqing.wechat"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

dependencies {
    // Feature modules
    implementation(project(":feature:startup"))
    implementation(project(":feature:home"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:discovery"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:call"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:common"))

    // Core modules
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:location"))
    implementation(project(":core:model"))

    // 核心库
    implementation(libs.core.ktx)
    implementation(libs.annotations)
    implementation(libs.appcompat)

    // 数据存储与序列化
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // 导航
    implementation(libs.navigation.compose)

    // Compose 核心 UI
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // 依赖注入 (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // 测试相关
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.junit.ext)
    androidTestImplementation(libs.test.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // 调试工具
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Java 8+ 脱糖支持
    coreLibraryDesugaring(libs.desugar.jdk)
}