plugins {
    id("multiloader-platform")
    id("net.neoforged.moddev") version("2.0.+")
}

base {
    archivesName = "koreanify-neoforge"
}

val configurationCommonModJava: Configuration = configurations.create("commonModJava") {
    isCanBeResolved = true
}
val configurationCommonModResources: Configuration = configurations.create("commonModResources") {
    isCanBeResolved = true
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))

    compileOnly("net.caffeinemc:sodium-neoforge-api:${BuildConfig.SODIUM_VERSION}")
}

sourceSets {
    main {
        compileClasspath += configurationCommonModJava
        runtimeClasspath += configurationCommonModJava
    }
}

neoForge {
    version = BuildConfig.NEOFORGE_VERSION

    runs {
        create("Client") {
            client()
            ideName = "NeoForge/Client"
        }
    }

    mods {
        create("koreanify") {
            sourceSet(sourceSets["main"])
            sourceSet(project(":common").sourceSets["main"])
        }
    }
}

tasks {
    val modsDir = rootProject.layout.buildDirectory.dir("mods")

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