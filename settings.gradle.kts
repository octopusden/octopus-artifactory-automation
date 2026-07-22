import java.net.InetAddress
import java.util.zip.CRC32

pluginManagement {
    val detektVersion: String by settings
    val ktlintGradleVersion: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm") version ("2.0.21")
        id("com.gradleup.shadow") version ("8.3.6")
        id("com.avast.gradle.docker-compose") version ("0.16.9")
        id("io.github.gradle-nexus.publish-plugin") version ("1.1.0")
        id("org.octopusden.octopus.oc-template") version (extra["octopus-oc-template.version"] as String)
        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintGradleVersion
        id("org.octopusden.octopus-quality") version (extra["octopus-quality.version"] as String)
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "octopus-artifactory-automation"

gradle.beforeProject {
    project.version = gradle.startParameter.projectProperties["version"] ?: with(CRC32()) {
        update(InetAddress.getLocalHost().hostName.toByteArray())
        "$value-SNAPSHOT"
    }
}
