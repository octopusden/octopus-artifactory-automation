pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version ("1.9.20")
        id("com.github.johnrengelman.shadow") version ("8.1.1")
        id("io.github.gradle-nexus.publish-plugin") version ("1.1.0")
    }
}

rootProject.name = "octopus-artifactory-automation"