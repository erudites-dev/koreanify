import me.modmuss50.mpp.PublishModTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

plugins {
    id("me.modmuss50.mod-publish-plugin")
}

val projectProviders = providers

fun envFlag(name: String, default: Boolean): Boolean = projectProviders.environmentVariable(name)
    .orElse(default.toString())
    .get()
    .toBoolean()

gradle.projectsEvaluated {
    // https://github.com/modmuss50/mod-publish-plugin
    publishMods {
        //dryRun = true

        val changelogEnv = projectProviders.environmentVariable("CHANGELOG_TEXT").orNull
        changelog = if (!changelogEnv.isNullOrEmpty()) {
            changelogEnv
        } else {
            projectProviders.fileContents(layout.projectDirectory.file("changelog.md")).asText.get()
        }

        val versionType = projectProviders.environmentVariable("VERSION_TYPE").orElse("release").get()
        type = when (versionType) {
            "alpha" -> ALPHA
            "beta" -> BETA
            else -> STABLE
        }
        val includeSnapshot = envFlag("INCLUDE_SNAPSHOTS", false)
        val publishEnabled = envFlag("PUBLISH_ENABLED", true)
        val fabricEnabled = envFlag("FABRIC_ARTIFACT", true)
        val neoforgeEnabled = envFlag("NEOFORGE_ARTIFACT", true)
        val modrinthEnabled = envFlag("PUBLISH_MODRINTH", true)
        val curseforgeEnabled = envFlag("PUBLISH_CURSEFORGE", true)

        if (!publishEnabled) {
            println("Publishing is disabled. Set PUBLISH_ENABLED=true to enable.")
            return@publishMods
        }

        val cfOptions = curseforgeOptions {
            accessToken = projectProviders.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1466540"
            minecraftVersionRange {
                start = BuildConfig.MINECRAFT_VERSION_MIN
                end = BuildConfig.MINECRAFT_PUBLISH_END
            }
            clientRequired = true
            javaVersions.add(JavaVersion.toVersion(BuildConfig.JAVA_VERSION))
        }

        val mrOptions = modrinthOptions {
            accessToken = projectProviders.environmentVariable("MODRINTH_TOKEN")
            projectId = "p1nSK3e3"
            minecraftVersionRange {
                start = BuildConfig.MINECRAFT_VERSION_MIN
                end = BuildConfig.MINECRAFT_PUBLISH_END
                includeSnapshots = includeSnapshot
            }
        }

        val minecraftVersion = BuildConfig.MINECRAFT_VERSION.substringBefore('-')
        val fabricJar = project(":fabric").tasks.named<Jar>("jar").flatMap { it.archiveFile }
        val neoforgeJar = project(":neoforge").tasks.named<Jar>("jar").flatMap { it.archiveFile }

        fun publishArtifact(
            loaderId: String,
            loaderName: String,
            jar: Provider<RegularFile>,
            vararg modLoaderIds: String,
        ) {
            val platformSuffix = loaderId.replaceFirstChar(Char::uppercaseChar)
            val artifactVersion = "mc${minecraftVersion}-${BuildConfig.MOD_VERSION}-${loaderId}"
            val artifactDisplayName = "Koreanify ${BuildConfig.MOD_VERSION} for ${loaderName} ${BuildConfig.MINECRAFT_VERSION}"

            if (curseforgeEnabled) {
                curseforge("curseforge${platformSuffix}") {
                    from(cfOptions)
                    file = jar
                    modLoaders.addAll(*modLoaderIds)
                    version = artifactVersion
                    displayName = artifactDisplayName
                }
            }

            if (modrinthEnabled) {
                modrinth("modrinth${platformSuffix}") {
                    from(mrOptions)
                    file = jar
                    modLoaders.addAll(*modLoaderIds)
                    version = artifactVersion
                    displayName = artifactDisplayName
                }
            }
        }

        if (fabricEnabled) {
            publishArtifact(
                "fabric",
                "Fabric",
                fabricJar,
                "fabric", "quilt"
            )
        }

        if (neoforgeEnabled) {
            publishArtifact(
                "neoforge",
                "NeoForge",
                neoforgeJar,
                "neoforge"
            )
        }

        // GitHub Release
        github {
            accessToken = projectProviders.environmentVariable("GITHUB_TOKEN")
            repository = "erudites-dev/koreanify"
            commitish = "main"
            tagName = "${BuildConfig.MOD_VERSION}+mc${BuildConfig.MINECRAFT_VERSION}"
            version = "${BuildConfig.MOD_VERSION}+mc${BuildConfig.MINECRAFT_VERSION}"
            displayName = "Koreanify ${BuildConfig.MOD_VERSION} for Minecraft ${BuildConfig.MINECRAFT_VERSION}"

            val jars = listOfNotNull(
                fabricJar.takeIf { fabricEnabled },
                neoforgeJar.takeIf { neoforgeEnabled },
            )
            jars.firstOrNull()?.let { file = it }
            jars.drop(1).forEach { additionalFiles.from(it) }
        }
    }

    val publishTasks = tasks.withType<PublishModTask>()
    val fabricPublishTasks = publishTasks.matching { it.platform.name.contains("fabric", ignoreCase = true) }
    publishTasks.matching { it.platform.name.contains("neoforge", ignoreCase = true) }.configureEach {
        mustRunAfter(fabricPublishTasks)
    }
}