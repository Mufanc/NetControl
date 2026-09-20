pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") {
            content { includeGroupByRegex("xyz\\.mufanc\\.aproc.*") }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content { includeGroupByRegex("xyz\\.mufanc\\.aproc.*") }
        }
    }
}

rootProject.name = "netc"

include(":daemon", ":hiddenapi")
