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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

}

rootProject.name = "GymApp"

include(":app")

// Core modules
include(":core:core-model")
include(":core:core-database")
include(":core:core-drivebackup")
include(":core:core-ai")
include(":core:core-camera")

// Feature modules
include(":features:feature-workouts")
include(":features:feature-history")
include(":features:feature-scan")
include(":features:feature-settings")
