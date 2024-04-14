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
include("auth:module")
include("auth:domain")
include("auth:data")
include("checklist:module")
include("checklist:domain")
include("checklist:data")
include("notification:module")
include("notification:domain")
include("notification:data")
