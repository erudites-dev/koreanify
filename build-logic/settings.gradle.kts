rootProject.name = "build-logic"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("mod-publish-plugin", "me.modmuss50", "mod-publish-plugin").version("2.0.0-beta.1")
        }
    }
}