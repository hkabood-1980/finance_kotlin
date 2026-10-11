pluginManagement {
    repositories {
        maven { url = uri("https://maven.myket.ir") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.myket.ir") }
        google()
        mavenCentral()
    }
}

rootProject.name = "finacemobile"
include(":app")