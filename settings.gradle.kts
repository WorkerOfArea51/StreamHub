pluginManagement {
    repositories {
        google()
        maven {
            url = java.net.URI("https://maven-central.storage-download.googleapis.com/maven2/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven {
            url = java.net.URI("https://maven-central.storage-download.googleapis.com/maven2/")
        }
        mavenCentral()
        maven {
            url = java.net.URI("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }
}

rootProject.name = "StreamHub"
include(":app")
