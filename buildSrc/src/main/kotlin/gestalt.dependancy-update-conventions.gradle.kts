import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

/**
 * Apply to your main gradle build script and run with:
 * .\gradlew dependencyUpdates -Drevision=release -DoutputFormatter=json,xml
 * to get a list of dependencies that may need to be updated.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
plugins {
    id("com.github.ben-manes.versions")
}


tasks.withType<DependencyUpdatesTask> {
    // disallow release candidates as upgradable versions from stable versions
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }

    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}