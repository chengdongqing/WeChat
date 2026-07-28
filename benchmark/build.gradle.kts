plugins {
    id("com.android.test")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "top.chengdongqing.wechat.benchmark"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.test.junit.ext)
}
