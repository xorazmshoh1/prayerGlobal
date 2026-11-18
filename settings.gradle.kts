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

rootProject.name = "NamozHifz"
include(
    ":core-common", ":core-ui", ":core-data",
    ":feature-prayer", ":feature-calendar", ":feature-hifz",
    ":feature-qibla", ":feature-tasbih", ":feature-ramadan",
    ":app-namoz-hifz",
)
