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
    resolutionStrategy {
        // Force Kotlin plugins to resolve from Maven Central at the specified version
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HomeApps"
include(":app")
