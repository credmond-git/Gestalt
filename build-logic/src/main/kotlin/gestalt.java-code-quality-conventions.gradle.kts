import net.ltgt.gradle.errorprone.errorprone

/**
 * Apply to all modules to include multiple code quality plugins to your module.
 */
plugins {
    id("net.ltgt.errorprone")
    checkstyle
    pmd
}

dependencies {
    errorprone(libs.errorProne)
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
}

checkstyle {
    toolVersion = libs.versions.checkStyle.get()
    configFile = file(rootDir.path + "/config/checkstyle/google_checks.xml")
    isIgnoreFailures = true
}

pmd {
    isConsoleOutput = true
    toolVersion = libs.versions.pmd.get()
    ruleSets = listOf(rootDir.path + "/config/pmd/custom_ruleset.xml")
}