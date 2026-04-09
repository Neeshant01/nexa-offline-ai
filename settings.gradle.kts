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

rootProject.name = "Nexa Offline AI"

include(
    ":app",
    ":core",
    ":core-ui",
    ":domain",
    ":data",
    ":ai",
    ":navigation",
    ":feature-home",
    ":feature-chat",
    ":feature-notes",
    ":feature-reminders",
    ":feature-settings",
    ":feature-voice",
    ":workers",
)
