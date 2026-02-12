import java.net.InetAddress
import java.util.zip.CRC32

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version ("2.0.21")
        id("com.gradleup.shadow") version ("8.3.6")
        id("com.avast.gradle.docker-compose") version ("0.16.9")
        id("io.github.gradle-nexus.publish-plugin") version ("1.1.0")
        id("org.octopusden.octopus.oc-template") version (extra["octopus-oc-template.version"] as String)
        id("com.jfrog.artifactory") version ("5.2.5")
    }
}

rootProject.name = "octopus-artifactory-automation"

gradle.beforeProject {
    project.version = gradle.startParameter.projectProperties["version"] ?: with(CRC32()) {
        update(InetAddress.getLocalHost().hostName.toByteArray())
        "$value-SNAPSHOT"
    }
}