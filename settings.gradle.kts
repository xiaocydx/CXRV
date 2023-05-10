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
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "CXRV"
include(":app")
include(":cxrv")
include(":cxrv-paging")
include(":cxrv-binding")
include(":cxrv-viewpager2")
include(":cxrv-animatable")
