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
    // 核心库
    implementation(libs.core.ktx)
    implementation(libs.annotations)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.pinyin)

    // 生命周期与导航
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    // Compose 核心 UI
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.material3.adaptive)

    // 权限管理
    implementation(libs.permissions.accompanist)

    // 依赖注入 (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    //数据库 (Room)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // 图片加载与多媒体
    implementation(libs.bundles.coil)
    implementation(libs.coil.zoomable)

    // 相机与扫码
    implementation(libs.bundles.camera)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)

    // 实时通话
    implementation(libs.webrtc)

    // 高德地图合包
    implementation(files("${rootProject.projectDir}/libs/Lite3dMap_1.2.0_AMapSearch_9.7.4_AMapLocation_11.1.000_20260306.aar"))

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