plugins {
    id("me.modmuss50.mod-publish-plugin")
}

val projectProviders = providers

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
        val includeSnapshot = projectProviders.environmentVariable("INCLUDE_SNAPSHOTS").orElse("false").get().toBoolean()
        val publishEnabled = projectProviders.environmentVariable("PUBLISH_ENABLED").orElse("true").get().toBoolean()
        val fabricEnabled = projectProviders.environmentVariable("FABRIC_ARTIFACT").orElse("true").get().toBoolean()
        val neoforgeEnabled = projectProviders.environmentVariable("NEOFORGE_ARTIFACT").orElse("true").get().toBoolean()
        val modrinthEnabled = projectProviders.environmentVariable("PUBLISH_MODRINTH").orElse("true").get().toBoolean()
        val curseforgeEnabled = projectProviders.environmentVariable("PUBLISH_CURSEFORGE").orElse("true").get().toBoolean()

        if (!publishEnabled) {
            println("Publishing is disabled. Set PUBLISH_ENABLED=true to enable.")
            return@publishMods
        }

        val cfOptions = curseforgeOptions {
            accessToken = projectProviders.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1466540"
            minecraftVersionRange {
                start = BuildConfig.MINECRAFT_VERSION_MIN
                end = "latest"
            }
            clientRequired = true
            javaVersions.add(JavaVersion.toVersion(BuildConfig.JAVA_VERSION))
        }

        val mrOptions = modrinthOptions {
            accessToken = projectProviders.environmentVariable("MODRINTH_TOKEN")
            projectId = "p1nSK3e3"
            minecraftVersionRange {
                start = BuildConfig.MINECRAFT_VERSION_MIN
                end = "latest"
                includeSnapshots = includeSnapshot
            }
        }

        val minecraftVersion = BuildConfig.MINECRAFT_VERSION.substringBefore('-')
        val fabricJar = project(":fabric").tasks.named<Jar>("jar").flatMap { it.archiveFile }
        val neoforgeJar = project(":neoforge").tasks.named<Jar>("jar").flatMap { it.archiveFile }

        // Fabric
        if (fabricEnabled) {
            if (curseforgeEnabled) {
                curseforge("curseforgeFabric") {
                    from(cfOptions)
                    file = fabricJar
                    modLoaders.add("fabric")
                    modLoaders.add("quilt")
                    version = "mc${minecraftVersion}-${BuildConfig.MOD_VERSION}-fabric"
                    displayName = "Koreanify ${BuildConfig.MOD_VERSION} for Fabric ${BuildConfig.MINECRAFT_VERSION}"
                }
            }

            if (modrinthEnabled) {
                modrinth("modrinthFabric") {
                    from(mrOptions)
                    file = fabricJar
                    modLoaders.add("fabric")
                    modLoaders.add("quilt")
                    version = "mc${minecraftVersion}-${BuildConfig.MOD_VERSION}-fabric"
                    displayName = "Koreanify ${BuildConfig.MOD_VERSION} for Fabric ${BuildConfig.MINECRAFT_VERSION}"
                }
            }
        }

        // NeoForge
        if (neoforgeEnabled) {
            if (curseforgeEnabled) {
                curseforge("curseforgeNeoforge") {
                    from(cfOptions)
                    file = neoforgeJar
                    modLoaders.add("neoforge")
                    version = "mc${minecraftVersion}-${BuildConfig.MOD_VERSION}-neoforge"
                    displayName = "Koreanify ${BuildConfig.MOD_VERSION} for NeoForge ${BuildConfig.MINECRAFT_VERSION}"
                }
            }

            if (modrinthEnabled) {
                modrinth("modrinthNeoforge") {
                    from(mrOptions)
                    file = neoforgeJar
                    modLoaders.add("neoforge")
                    version = "mc${minecraftVersion}-${BuildConfig.MOD_VERSION}-neoforge"
                    displayName = "Koreanify ${BuildConfig.MOD_VERSION} for NeoForge ${BuildConfig.MINECRAFT_VERSION}"
                }
            }
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

    tasks.matching { it.name.startsWith("publish") && it.name.contains("Neoforge", ignoreCase = true) }.configureEach {
        mustRunAfter(tasks.matching { it.name.startsWith("publish") && it.name.contains("Fabric", ignoreCase = true) })
    }
}