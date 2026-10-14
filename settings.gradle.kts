pluginManagement {
    repositories {
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}

rootProject.name = "finacemobile"
include(":app")