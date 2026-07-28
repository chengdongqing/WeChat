@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.arm.aichat"
    compileSdk {
        version = release(37)
    }

    ndkVersion = "29.0.13113456"

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DCMAKE_MESSAGE_LOG_LEVEL=DEBUG"
                arguments += "-DCMAKE_VERBOSE_MAKEFILE=ON"

                arguments += "-DBUILD_SHARED_LIBS=ON"
                arguments += "-DLLAMA_BUILD_APP=OFF"
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DLLAMA_OPENSSL=OFF"

                arguments += "-DGGML_NATIVE=OFF"
                arguments += "-DGGML_BACKEND_DL=ON"
                arguments += "-DGGML_CPU_ALL_VARIANTS=ON"
                arguments += "-DGGML_LLAMAFILE=OFF"
            }
        }
        aarMetadata {
            minCompileSdk = 35
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        jniLibs {
            excludes += setOf(
                "**/libggml-cpu-android_armv8.0_1.so",
                "**/libggml-cpu-android_armv8.2_1.so",
                "**/libggml-cpu-android_armv8.2_2.so",
                "**/libggml-cpu-android_armv8.6_1.so",
                "**/libggml-cpu-android_armv9.0_1.so",
                "**/libggml-cpu-android_armv9.2_1.so"
            )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
}
