plugins {
    alias(libs.plugins.android.library)
    id("kotlin-parcelize")
}

android {
    namespace = "top.chengdongqing.wechat.core.media"
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
}

dependencies {
    implementation(projects.core.file)
    implementation(libs.core.ktx)
    implementation(libs.exifinterface)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
}
