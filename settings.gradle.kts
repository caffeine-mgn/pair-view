pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven(url = "https://repo.binom.pw")
        google()
        mavenCentral()
    }
}
rootProject.name = "RayNeoARSDKAndroidDemo"
include(":app")
include(":shared")
include(":client")
include(":server")
include(":frontend")
