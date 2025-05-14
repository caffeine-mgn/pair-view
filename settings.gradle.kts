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
include(":deviceClient")
include(":server")
include(":deviceShared")
include(":agent")
include(":glasses")
//include(":frontend")
include(":serverShared")
