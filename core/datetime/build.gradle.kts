plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "top.chengdongqing.wechat.core.datetime"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    compileSdkMinor = 0
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
}
