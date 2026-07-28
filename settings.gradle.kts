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
include(":core:navigation")
include(":core:database")
include(":core:designsystem")
include(":core:common")
include(":core:network")
include(":core:location")
include(":core:data")
include(":core:ai")

// Feature modules
include(":feature:launch")
include(":feature:home")
include(":feature:chat")
include(":feature:contacts")
include(":feature:discovery")
include(":feature:profile")
include(":feature:call")
include(":feature:settings")
include(":feature:common")
