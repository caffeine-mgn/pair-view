pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://repo.binom.pw")
        gradlePluginPortal()
        google()
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
include(":phone")
include(":phone-shared")
include(":android-common")
include(":shared")
//include(":deviceClient")
include(":server")
include(":glasses-shared")
include(":deviceShared")
include(":agent")
include(":glasses")
//include(":frontend")
include(":serverShared")
