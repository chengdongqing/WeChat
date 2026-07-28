// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("verifyModuleBoundaries") {
    group = "verification"
    description = "Fails when a module crosses an architectural dependency boundary."

    doLast {
        val featureProjects = subprojects
            .map { it.path }
            .filter { it.startsWith(":feature:") }
            .toSet()
        val violations = mutableListOf<String>()

        subprojects.forEach { project ->
            val dependencies: Set<String> = project.configurations
                .flatMap { configuration ->
                    configuration.dependencies
                        .filterIsInstance<ProjectDependency>()
                        .map { dependency -> dependency.path }
                }
                .toSet()

            dependencies.filterNot { it == project.path }.forEach { dependency ->
                when {
                    project.path.startsWith(":feature:") && dependency in featureProjects ->
                        violations += "${project.path} must not depend on feature module $dependency"
                    project.path.startsWith(":core:") && dependency in featureProjects ->
                        violations += "${project.path} must not depend on feature module $dependency"
                    project.path in setOf(":core:data", ":core:network") &&
                        dependency == ":core:designsystem" ->
                        violations += "${project.path} must not depend on :core:designsystem"
                }
            }
        }

        check(violations.isEmpty()) {
            "Module boundary violations:\n${violations.joinToString("\n")}"
        }
    }
}
