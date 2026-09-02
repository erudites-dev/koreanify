plugins {
    id("multiloader-platform")
    id("net.fabricmc.fabric-loom") version("1.17.+")
}

base {
    archivesName = "koreanify-fabric"
}

val configurationCommonModJava: Configuration = configurations.create("commonJava") {
    isCanBeResolved = true
}
val configurationCommonModResources: Configuration = configurations.create("commonResources") {
    isCanBeResolved = true
}

repositories {
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))
}

sourceSets.apply {
    main {
        compileClasspath += configurationCommonModJava
        runtimeClasspath += configurationCommonModJava
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    implementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    compileOnly("net.caffeinemc:sodium-fabric-api:${BuildConfig.SODIUM_VERSION}")
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/koreanify.classtweaker")

    mixin {
        useLegacyMixinAp = false
    }

    runs {
        named("client") {
            client()
            configName = "Fabric/Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

tasks {
    val modsDir = rootProject.layout.buildDirectory.dir("mods")

    // :common and :fabric share one loom cache, and genSources line-maps the merged jar in place.
    named("validateAccessWidener") {
        mustRunAfter(":common:genSourcesWithVineflower")
    }

    jar {
        from(configurationCommonModJava)
        destinationDirectory.set(modsDir)
    }

    sourcesJar {
        from(configurationCommonModJava)
        destinationDirectory.set(modsDir.map { it.dir("sources") })
    }

    processResources {
        from(configurationCommonModResources)
    }
}