@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// 开启类型安全项目访问器
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "WeChat"
include(":app")
include(":benchmark")

// Core modules
include(":core:model")
include(":core:util")
include(":core:navigation")
include(":core:database")
include(":core:designsystem")
include(":core:network")
include(":core:location")
include(":core:data")
include(":core:ai")
include(":core:file")
include(":core:media")
include(":core:media-ui")
include(":core:camera")
include(":core:call-ui")
include(":core:connectivity")
include(":core:playback")
include(":core:proximity-ui")
include(":core:qrcode")
include(":core:notification")
include(":core:security")
include(":core:datetime")
include(":core:app-picker")
include(":core:runtime")
include(":core:nfc")

// Feature modules
include(":feature:launch")
include(":feature:auth")
include(":feature:chat")
include(":feature:contacts")
include(":feature:discovery")
include(":feature:moments")
include(":feature:intercom")
include(":feature:profile")
include(":feature:favorites")
include(":feature:call")
include(":feature:settings")
include(":feature:common")
