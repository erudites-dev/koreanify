pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "koreanify"

includeBuild("build-logic")
include("common")
include("fabric")
include("neoforge")