pluginManagement {
    repositories {
        google()
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

rootProject.name = "PhonePulse"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:model")
include(":core:database")
include(":feature:diagnostic")
include(":feature:certificate")
include(":feature:scanner")
include(":feature:history")
include(":feature:onboarding")
include(":feature:home")
