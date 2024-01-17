pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
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

rootProject.name = "checkstory-backend-ktor"
include("app")
include("core:monolith")
include("core:domain")
include("template:module")
include("template:domain")
include("template:data")
