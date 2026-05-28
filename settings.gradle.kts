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

rootProject.name = "ReelGrab"

include(":app")
include(":core:ui")
include(":core:network")
include(":core:common")
include(":domain")
include(":data")
include(":feature:home")
include(":feature:preview")
include(":feature:history")
include(":feature:settings")
