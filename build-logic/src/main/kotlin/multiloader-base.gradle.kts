plugins {
    id("java-library")
    id("idea")
}

group = "dev.erudites"
version = BuildConfig.createVersionString(project)

java.toolchain.languageVersion = JavaLanguageVersion.of(BuildConfig.JAVA_VERSION)

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(BuildConfig.JAVA_VERSION)
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "CaffeineMC"
                url = uri("https://maven.caffeinemc.net/releases")
            }
        }
        filter {
            includeGroup("net.caffeinemc")
        }
    }
}