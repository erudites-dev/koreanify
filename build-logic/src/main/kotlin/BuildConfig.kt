import org.gradle.api.Project

object BuildConfig {
    val JAVA_VERSION: Int = 25

    val MINECRAFT_VERSION_RANGE: String = ">=26.1" // range: ">=26.1 <27.1"
    val MINECRAFT_VERSION_MIN: String = MINECRAFT_VERSION_RANGE.split(" ")[0].replace(Regex("^[><=!\\[\\]()]+"), "")
    val MINECRAFT_VERSION: String = "26.1"
    val NEOFORGE_VERSION: String = "26.1.0.1-beta"
    val FABRIC_LOADER_VERSION: String = "0.18.4"

    // https://semver.org/
    var MOD_VERSION: String = "0.1.0"

    fun createVersionString(project: Project): String {
        val builder = StringBuilder()

        val isReleaseBuild = project.hasProperty("build.release")
        val buildId = System.getenv("GITHUB_RUN_NUMBER")

        if (isReleaseBuild) {
            builder.append(MOD_VERSION)
        } else {
            builder.append(MOD_VERSION.substringBefore('-'))
            builder.append("-snapshot")
        }

        builder.append("+mc").append(MINECRAFT_VERSION)

        if (!isReleaseBuild) {
            if (buildId != null) {
                builder.append("-build.${buildId}")
            } else {
                builder.append("-local")
            }
        }

        return builder.toString()
    }
}